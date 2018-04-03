package net.stacksmashing.sechat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.stacksmashing.sechat.db.Contact;
import net.stacksmashing.sechat.voice.CallHandler;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.hdodenhof.circleimageview.CircleImageView;

class CallParticipantsAdapter extends BaseAdapter {
    private final Context context;
    private final boolean withCaller;

    public CallParticipantsAdapter(Context context) {
        this(context, true);
    }

    public CallParticipantsAdapter(Context context, boolean withCaller) {
        this.context = context;
        this.withCaller = withCaller;
    }

    private List<CallHandler.Participant> getParticipants() {
        List<CallHandler.Participant> participants = CallHandler.INSTANCE.getParticipants();

        if (participants == null) {
            return new ArrayList<>();
        }

        if (withCaller || CallHandler.INSTANCE.getCaller() == null) {
            return participants;
        }

        // FIXME: This is ugly, and is not a good thing to do repeatedly.
        List<CallHandler.Participant> withoutCaller = new ArrayList<>(participants.size() - 1);
        for (CallHandler.Participant participant : participants) {
            if (!participant.getName().equals(CallHandler.INSTANCE.getCaller())) {
                withoutCaller.add(participant);
            }
        }
        return withoutCaller;
    }

    @Override
    public int getCount() {
        return getParticipants().size();
    }

    @Override
    public Object getItem(int i) {
        return getParticipants().get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.activity_call_participants_list_item, viewGroup, false);
            view.setTag(new ViewHolder(view));
        }

        CallHandler.Participant participant = getParticipants().get(i);
        ((ViewHolder) view.getTag()).bind(context, participant);

        return view;
    }

    static class ViewHolder {
        @InjectView(R.id.activity_call_participants_list_item_picture)
        CircleImageView picture;

        @InjectView(R.id.activity_call_participants_list_item_name)
        TextView name;

        @InjectView(R.id.activity_call_participants_list_item_status)
        TextView status;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

        void bind(Context context, CallHandler.Participant participant) {
            name.setText(participant.getName());
            status.setText(participant.getStatus().descriptionId());

            Contact contact = Contact.findContactByUsername(context, participant.getName());
            if (contact != null) {
                contact.loadProfilePictureInto(context, picture);
            }
        }
    }
}
