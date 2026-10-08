package net.riftwatch.rift_quests.network;

public final class ClientBridge {
    public interface Handler {
        void openBook();

        void book(Payloads.Book payload);

        void progress(Payloads.Progress payload);

        void questCompleted(Payloads.QuestCompleted payload);
    }

    private static final Handler NONE = new Handler() {
        @Override
        public void openBook() {
        }

        @Override
        public void book(Payloads.Book payload) {
        }

        @Override
        public void progress(Payloads.Progress payload) {
        }

        @Override
        public void questCompleted(Payloads.QuestCompleted payload) {
        }
    };

    private static volatile Handler handler = NONE;

    private ClientBridge() {
    }

    public static Handler handler() {
        return handler;
    }

    public static void install(Handler client) {
        handler = client;
    }
}
