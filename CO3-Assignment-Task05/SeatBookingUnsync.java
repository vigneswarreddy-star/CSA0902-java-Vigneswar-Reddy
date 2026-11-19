public class SeatBookingUnsync {
    private final boolean[] booked;
    private int bookingsMade = 0;

    SeatBookingUnsync(int seats) { booked = new boolean[seats]; }

    // TODO 1 completed: check-then-book is one atomic operation (intrinsic lock on this object)
    void book(int seat) {   // NO synchronized: TODO 1 left as written here
        if (!booked[seat]) {
            booked[seat] = true;
            bookingsMade++;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SeatBookingUnsync screen = new SeatBookingUnsync(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;                     // 10 users compete for every seat
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}
