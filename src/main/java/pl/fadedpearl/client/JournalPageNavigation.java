package pl.fadedpearl.client;

final class JournalPageNavigation {
    private JournalPageNavigation() {}

    static int maxStartPage(int pageCount, boolean twoPage) {
        int lastPage = Math.max(0, pageCount - 1);
        return twoPage ? lastPage - lastPage % 2 : lastPage;
    }

    static int clampStartPage(int requested, int pageCount, boolean twoPage) {
        int clamped = Math.max(0, Math.min(maxStartPage(pageCount, twoPage), requested));
        return twoPage ? clamped - clamped % 2 : clamped;
    }

    static int move(int current, int direction, int pageCount, boolean twoPage) {
        return clampStartPage(current + direction * (twoPage ? 2 : 1), pageCount, twoPage);
    }
}
