import java.util.Comparator;
import java.util.PriorityQueue;

class Patient {
    final String name; final int severity; final long arrival;
    Patient(String name, int severity, long arrival) {
        this.name = name; this.severity = severity; this.arrival = arrival;
    }
    @Override public String toString() { return name + "(S" + severity + ")"; }
}

public class Triage {
    private long counter = 0;

    // TODO 1 completed: lowest severity number first; for equal severity, earliest arrival first
    private final PriorityQueue<Patient> queue = new PriorityQueue<>(
            Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));

    void admit(String name, int severity) {
        queue.add(new Patient(name, severity, counter++));
    }

    Patient next() { return queue.poll(); }

    /** Waiting list shown to staff: the heap array order is NOT the treatment order, so drain a copy. */
    String waitingListInTreatmentOrder() {
        PriorityQueue<Patient> copy = new PriorityQueue<>(queue);
        StringBuilder sb = new StringBuilder();
        Patient p;
        while ((p = copy.poll()) != null) sb.append(p).append(' ');
        return sb.toString().trim();
    }

    String rawQueueView() { return queue.toString(); }

    public static void main(String[] args) {
        // Original example from the brief
        Triage er = new Triage();
        er.admit("Ravi", 3); er.admit("Meena", 1); er.admit("John", 3);
        er.admit("Asha", 5); er.admit("Kumar", 1); er.admit("Lata", 2);
        StringBuilder order = new StringBuilder();
        Patient p;
        while ((p = er.next()) != null) order.append(p).append(' ');
        System.out.println(order.toString().trim());

        // Hand-verified check with 8 patients (arrival order in brackets)
        // Ravi(3)[0] Meena(1)[1] John(3)[2] Asha(5)[3] Kumar(1)[4] Lata(2)[5] Dev(2)[6] Nita(4)[7]
        // By hand: S1 Meena, Kumar; S2 Lata, Dev; S3 Ravi, John; S4 Nita; S5 Asha
        String expected8 = "Meena(S1) Kumar(S1) Lata(S2) Dev(S2) Ravi(S3) John(S3) Nita(S4) Asha(S5)";
        Triage ward = new Triage();
        ward.admit("Ravi", 3); ward.admit("Meena", 1); ward.admit("John", 3); ward.admit("Asha", 5);
        ward.admit("Kumar", 1); ward.admit("Lata", 2); ward.admit("Dev", 2); ward.admit("Nita", 4);
        System.out.println("\n8-patient check");
        System.out.println("  raw PriorityQueue (heap array, NOT treatment order): " + ward.rawQueueView());
        String got8 = ward.waitingListInTreatmentOrder();
        System.out.println("  waiting list in treatment order: " + got8);
        System.out.println("  expected:                        " + expected8);
        System.out.println("  result: " + (expected8.equals(got8) ? "PASS" : "FAIL"));
    }
}
