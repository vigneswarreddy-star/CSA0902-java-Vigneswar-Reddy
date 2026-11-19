# Task 5 (Q5) – Race Conditions in Seat Booking

- `SeatBooking.java` – Complete-the-Code program: TODO 1 completed with `synchronized`. Expected: `Seats sold: 10`.
- `SeatBookingUnsync.java` – the same program without `synchronized` (run it 10 times; see `results/seatbooking_unsync_10_runs.txt`).
- `RaceDemo.java` – reproduces double booking: a start gate releases all users together, and a `yield` between check and write widens the window.
- `BookingStrategies.java` – 10,000 bookings: synchronized method, synchronized block, no lock.
- `AltBookingBench.java` – AtomicBoolean CAS, ReentrantLock, ConcurrentHashMap putIfAbsent, synchronized method, for 100 and 10,000 seats.

Run: `javac -d . *.java` then run the main class you need.
