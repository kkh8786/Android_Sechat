package net.stacksmashing.sechat;

import android.os.Handler;
import android.os.Looper;

import net.stacksmashing.sechat.network.ServerAddContactMessage;
import net.stacksmashing.sechat.network.ServerRegisterMessage;
import net.stacksmashing.sechat.network.ServerRegisterPhoneNumberResponseMessage;
import net.stacksmashing.sechat.network.ServerVerifyPhoneNumberResponseMessage;
import net.stacksmashing.sechat.video.StreamManager;
import net.stacksmashing.sechat.voice.CallHandler;

import java.util.List;

public final class Bus {
    private static final Bus BUS = new Bus();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final com.squareup.otto.Bus bus = new com.squareup.otto.Bus();

    public static Bus bus() {
        return BUS;
    }

    private Bus() {
    }

    public void register(Object o) {
        bus.register(o);
    }

    public void unregister(Object o) {
        bus.unregister(o);
    }

    public void post(final Object o) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                bus.post(o);
            }
        });
    }

    public static class IsTypingEvent {
        private final String username;
        private final long chatId;

        public IsTypingEvent(String username, long chatId) {
            this.username = username;
            this.chatId = chatId;
        }

        public String getUsername() {
            return username;
        }

        public long getChatId() {
            return chatId;
        }
    }

    public static class RegistrationResultEvent {
        private final ServerRegisterMessage message;

        public RegistrationResultEvent(ServerRegisterMessage message) {
            this.message = message;
        }

        public boolean isSuccessful() {
            return message.isSuccessful();
        }

        public String getError() {
            return message.getError();
        }
    }

    public static class PhoneNumberRegistrationResultEvent {
        private final ServerRegisterPhoneNumberResponseMessage message;

        public PhoneNumberRegistrationResultEvent(ServerRegisterPhoneNumberResponseMessage message) {
            this.message = message;
        }

        public boolean isSuccessful() {
            return message.isSuccessful();
        }

        public String getError() {
            return message.getError();
        }
    }

    public static class PhoneNumberVerificationResultEvent {
        private final ServerVerifyPhoneNumberResponseMessage message;

        public PhoneNumberVerificationResultEvent(ServerVerifyPhoneNumberResponseMessage message) {
            this.message = message;
        }

        public boolean isSuccessful() {
            return message.isSuccessful();
        }
    }

    public static class AddContactEvent {
        private final ServerAddContactMessage message;

        public AddContactEvent(ServerAddContactMessage message) {
            this.message = message;
        }

        public boolean isSuccessful() {
            return message.isSuccessful();
        }

        public String getUsername() {
            return message.getUsername();
        }

        public String getError() {
            return message.getError();
        }

        public String getUserX() {
            return message.getUserX();
        }

        public String getUserY() {
            return message.getUserY();
        }
    }

    public static class OnlineStatusUpdateEvent {
    }

    public static class CallStateChangedEvent {
        private final CallHandler.State state;

        public CallStateChangedEvent(CallHandler.State state) {
            this.state = state;
        }

        public CallHandler.State getState() {
            return state;
        }
    }

    public static class CallUserStatusChangedEvent {
        private final String username;
        private final CallHandler.UserStatus status;

        public CallUserStatusChangedEvent(String username, CallHandler.UserStatus status) {
            this.username = username;
            this.status = status;
        }

        public String getUsername() {
            return username;
        }

        public CallHandler.UserStatus getStatus() {
            return status;
        }
    }

    public static class GroupChatCreatedEvent {
        private final long chatId;

        public GroupChatCreatedEvent(long chatId) {
            this.chatId = chatId;
        }

        public long getChatId() {
            return chatId;
        }
    }

    public static class OutgoingMessageEnqueuedEvent {
    }

    public static class VideoStreamStartEvent {
        private final StreamManager.StreamInfo streamInfo;
        private final String name;

        public VideoStreamStartEvent(String name, StreamManager.StreamInfo streamInfo) {
            this.name = name;
            this.streamInfo = streamInfo;
        }

        public StreamManager.StreamInfo getStreamInfo() {
            return streamInfo;
        }

        public String getName() {
            return name;
        }
    }

    public static class VideoStreamDataEvent {
        private final String name;
        private final byte[] data;

        public VideoStreamDataEvent(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }

        public String getName() {
            return name;
        }
    }

    public static class VideoStreamEndEvent {
        private final String name;

        public VideoStreamEndEvent(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class ChannelListEvent {
        private final List<String> channels;

        public ChannelListEvent(List<String> channels) {
            this.channels = channels;
        }

        public List<String> getChannels() {
            return channels;
        }
    }
}
