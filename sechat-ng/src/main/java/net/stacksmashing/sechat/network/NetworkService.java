package net.stacksmashing.sechat.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.squareup.otto.Subscribe;

import net.stacksmashing.sechat.Bus;
import net.stacksmashing.sechat.Preferences;
import net.stacksmashing.sechat.R;
import net.stacksmashing.sechat.db.Chat;
import net.stacksmashing.sechat.db.Message;
import net.stacksmashing.sechat.db.OutgoingMessage;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public final class NetworkService extends Service {
    private static final String TAG = "NetworkService";

    private static final String DEFAULT_HOST = "178.22.66.217";
    private static final int DEFAULT_PORT = 8002;

    private static final long PING_INTERVAL = 10000l;
    private static final long RECONNECT_INTERVAL = 5000l;

    private static final String EXTRA_REGISTER = "REGISTER";
    private static final String EXTRA_USERNAME = "USERNAME";
    private static final String EXTRA_DEVICE_NAME = "DEVICE_NAME";
    private static final String EXTRA_EC_X = "EC_X";
    private static final String EXTRA_EC_Y = "EC_Y";

    enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        LOGGED_IN
    }

    private static NetworkService INSTANCE = null;

    public static NetworkService getInstance() {
        return INSTANCE;
    }

    public static Intent intentWithRegistration(Context context, String username, String deviceName, String ecX, String ecY) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra(EXTRA_REGISTER, true);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_DEVICE_NAME, deviceName);
        intent.putExtra(EXTRA_EC_X, ecX);
        intent.putExtra(EXTRA_EC_Y, ecY);
        return intent;
    }

    private final PingSender pingSender = new PingSender(this);

    private Socket socket;
    private ReceiverThread receiverThread;
    private HandlerThread handlerThread;
    private FileSenderThread fileSenderThread;
    private Handler handler;
    private long session;

    private State state = State.DISCONNECTED;

    private boolean processingQueue = false;

    public State getState() {
        return state;
    }

    void setState(State state) {
        State oldState = this.state;
        this.state = state;
        Log.d(TAG, "State: " + oldState + " -> " + state);

        if (this.state == State.LOGGED_IN) {
            asyncProcessMessageQueue();
            Message.scheduleMessageDeletion(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        INSTANCE = this;

        handlerThread = new HandlerThread("NetworkService");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        fileSenderThread = new FileSenderThread(this);

        Bus.bus().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        asyncConnect(DEFAULT_HOST, DEFAULT_PORT, getInitialMessage(intent));
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Bus.bus().unregister(this);

        INSTANCE = null;

        handler = null;
        handlerThread.quit();

        receiverThread.interrupt();

        fileSenderThread.quit();
        fileSenderThread = null;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private ClientMessage getInitialMessage(Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_REGISTER)) {
            String username = intent.getStringExtra(EXTRA_USERNAME);
            String userX = intent.getStringExtra(EXTRA_EC_X);
            String userY = intent.getStringExtra(EXTRA_EC_Y);
            String deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
            return new ClientRegisterMessage(username, userX, userY, deviceName, userX, userY);
        }
        else if (Preferences.isRegistered(this)) {
            return new ClientLoginInitialMessage(Preferences.getUsername(this), Preferences.getDeviceName(this));
        }
        return null;
    }

    private boolean isConnected() {
        return state == State.CONNECTED || state == State.LOGGED_IN;
    }

    private void connect(String host, int port, ClientMessage message) {
        if (state != State.DISCONNECTED) {
            asyncSend(message);
            return;
        }

        try {
            setState(State.CONNECTING);

            socket = createSslContext().getSocketFactory().createSocket(host, port);

            receiverThread = new ReceiverThread(socket.getInputStream(), this);
            receiverThread.start();

            session = System.currentTimeMillis();

            setState(State.CONNECTED);

            asyncSend(message);

            schedulePing();
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to connect", e);
            disconnect();
        }
    }

    private SSLContext createSslContext() throws Exception {
        try {
            KeyStore store = KeyStore.getInstance("BKS");
            InputStream trustStore = getResources().openRawResource(R.raw.trust);
            store.load(trustStore, "PASSWORD".toCharArray());
            trustStore.close();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            tmf.init(store);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), new SecureRandom());
            return context;
        }
        catch (Exception e) {
            Log.e(TAG, "Could not create a SSL context", e);
            throw e;
        }
    }

    private void schedulePing() {
        handler.postDelayed(pingSender, PING_INTERVAL);
    }

    private void asyncConnect(final String host, final int port, final ClientMessage message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                connect(host, port, message);
            }
        });
    }

    private void disconnect() {
        if (state == State.DISCONNECTED) {
            return;
        }

        handler.removeCallbacks(pingSender);

        setState(State.DISCONNECTED);

        if (receiverThread != null) {
            receiverThread.interrupt();
            receiverThread = null;
        }

        if (socket != null) {
            try {
                socket.close();
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to close socket", e);
            }
            socket = null;
        }

        handler.removeCallbacksAndMessages(null);

        processingQueue = false;

        scheduleReconnect();
    }

    private void scheduleReconnect() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connect(DEFAULT_HOST, DEFAULT_PORT, getInitialMessage(null));
            }
        }, RECONNECT_INTERVAL);
    }

    public void asyncDisconnect() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        });
    }

    private void send(ClientMessage message) {
        if (message == null || !isConnected()) {
            return;
        }

        try {
            byte[] messageBytes = MsgPackUtil.bytesFromMessage(message);
            int size = messageBytes.length;
            byte[] data = new byte[12 + size];
            data[0] = 'Y';
            data[1] = 'O';
            data[2] = 'L';
            data[3] = 'O';
            data[4] = (byte) ((size >> 24) & 0xff);
            data[5] = (byte) ((size >> 16) & 0xff);
            data[6] = (byte) ((size >> 8) & 0xff);
            data[7] = (byte) (size & 0xff);
            data[8] = data[9] = data[10] = data[11] = 0;
            System.arraycopy(messageBytes, 0, data, 12, size);
            socket.getOutputStream().write(data);
        }
        catch (IOException e) {
            Log.e(TAG, "Could not send data", e);
            disconnect();
        }
    }

    public void asyncSend(final ClientMessage message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                send(message);
            }
        });
    }

    private void asyncProcessMessageQueue() {
        fileSenderThread.sendNextPart();
        handler.post(new Runnable() {
            @Override
            public void run() {
                processMessageQueue();
            }
        });
    }

    private void processMessageQueue() {
        if (processingQueue) {
            return;
        }

        processingQueue = true;

        Log.d(TAG, "[Q] Starting to process the outgoing message queue");

        processNextMessageFromQueue();
    }

    private void processNextMessageFromQueue() {
        OutgoingMessage message = OutgoingMessage.findNextOutgoingMessage(this, session);

        if (message == null || getState() != State.LOGGED_IN) {
            if (message == null) {
                Log.d(TAG, "[Q] No more messages in the queue");
            }
            else {
                Log.d(TAG, "[Q] We are no longer logged in - not processing the message queue");
            }
            processingQueue = false;
            return;
        }

        Log.d(TAG, "[Q] Next message " + message.getId() + " priority " + message.getLocalPriority());

        send(message.asClientMessage());

        /* Decrease the priority after each attempt at sending, ensuring that we try to send new messages before old ones. */
        message.setSession(session);
        OutgoingMessage.DAO.update(this, message);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processNextMessageFromQueue();
            }
        }, 50);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleEnqueuedOutgoingMessage(Bus.OutgoingMessageEnqueuedEvent event) {
        asyncProcessMessageQueue();
    }

    public void asyncSendFile(Chat chat, long deleteIn, String filename, Message.Type type, String localUuid) {
        fileSenderThread.send(chat, deleteIn, filename, type, localUuid);
    }

    public void asyncCopyAndSendFile(Uri uri, Chat chat, long deleteIn, String filename, Message.Type type, String localUuid) {
        fileSenderThread.copyToLocalStorageAndSend(uri, chat, deleteIn, filename, type, localUuid);
    }

    private static class ReceiverThread extends Thread {
        private final InputStream stream;
        private final WeakReference<NetworkService> serviceRef;

        ReceiverThread(InputStream stream, NetworkService service) {
            this.stream = stream;
            this.serviceRef = new WeakReference<>(service);
            setName("NetworkReceiver");
        }

        @Override
        public void run() {
            byte[] header = new byte[12];
            while (!isInterrupted()) {
                if (readFull(header) == -1) {
                    Log.d(TAG, "Error while reading header");
                    break;
                }

                if (header[0] != 'Y' || header[1] != 'O' || header[2] != 'L' || header[3] != 'O') {
                    Log.e(TAG, "Invalid frame header");
                    break;
                }

                int length = ((header[4] & 0xff) << 24) | ((header[5] & 0xff) << 16) | ((header[6] & 0xff) << 8) | header[7] & 0xff;

                byte[] messageBuffer = new byte[length];
                if (readFull(messageBuffer) == -1) {
                    Log.d(TAG, "Error while reading message");
                    break;
                }

                try {
                    ServerMessage message = MsgPackUtil.serverMessageFromBuffer(messageBuffer);

                    if (message != null) {
                        NetworkService service = serviceRef.get();
                        if (service != null) {
                            message.performAction(service);
                        }
                        else {
                            break;
                        }
                    }
                }
                catch (IOException e) {
                    Log.e(TAG, "Failed to decode message", e);
                }
            }
            Log.e(TAG, "Interrupted or network error");
            NetworkService networkService = serviceRef.get();
            if (networkService != null) {
                networkService.asyncDisconnect();
            }
        }

        private int readFull(byte[] buf) {
            int result = 0;
            while (result != -1 && result < buf.length) {
                try {
                    result += stream.read(buf, result, buf.length - result);
                }
                catch (IOException e) {
                    return -1;
                }
            }
            return result;
        }
    }

    private static class PingSender implements Runnable {
        private final WeakReference<NetworkService> serviceRef;

        private PingSender(NetworkService service) {
            this.serviceRef = new WeakReference<>(service);
        }

        @Override
        public void run() {
            NetworkService service = serviceRef.get();
            if (service != null) {
                service.asyncSend(new ClientPingMessage());
                service.schedulePing();
            }
        }
    }
}
