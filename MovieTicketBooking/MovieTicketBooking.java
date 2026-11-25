import java.io.*;
import java.util.*;

public class MovieTicketBooking {

    static final int ROWS = 5, COLS = 5;
    static final String FILE = "tickets.txt";

    static String[][] seats = new String[ROWS][COLS];
    static HashMap<String, Booking> bookings = new HashMap<>();
    static PriorityQueue<Waiting> waitList = new PriorityQueue<>();
    static int nextId = 101; 
    static long arrival = 0;

    static class Booking { 
        String id, name, seat;
        Booking(String id, String name, String seat) {
            this.id = id; this.name = name; this.seat = seat;
        }
    }

    static class Waiting implements Comparable<Waiting> {
        String name; boolean gold; long order;
        Waiting(String name, boolean gold, long order) {
            this.name = name; this.gold = gold; this.order = order;
        }
        String type() { return gold ? "Gold" : "Regular"; }
        public int compareTo(Waiting o) {
            if (gold != o.gold) return gold ? -1 : 1;
            return Long.compare(order, o.order);
        }
    }

    static String seatName(int r, int c) { return "" + (char) ('A' + r) + (c + 1); }

    static int[] parseSeat(String s) {
        if (s == null) return null;
        s = s.trim().toUpperCase();
        if (s.length() != 2) return null;
        int r = s.charAt(0) - 'A';
        int c = s.charAt(1) - '1';
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return null;
        return new int[]{r, c};
    }

    static boolean isFull() {
        for (String[] row : seats)
            for (String s : row)
                if (s == null) return false;
        return true;
    }

    static String createBooking(String name, int r, int c) {
        String id = "T" + nextId++;
        seats[r][c] = id;
        bookings.put(id, new Booking(id, name, seatName(r, c)));
        return id;
    }

    static void viewSeats() {
        System.out.println("\n    1  2  3  4  5");
        for (int r = 0; r < ROWS; r++) {
            StringBuilder sb = new StringBuilder("" + (char) ('A' + r) + "  ");
            for (int c = 0; c < COLS; c++)
                sb.append(seats[r][c] == null ? "[ ]" : "[X]");
            System.out.println(sb);
        }
        System.out.println("[ ] free   [X] booked");
    }

    static void bookSeat(Scanner sc) {
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }

        if (isFull()) {
            System.out.print("Category (Regular/Gold): ");
            boolean gold = sc.nextLine().trim().equalsIgnoreCase("Gold");
            Waiting w = new Waiting(name, gold, arrival++);
            waitList.add(w);
            System.out.println("Hall full. " + name + " added to waiting list (" + w.type() + ").");
            return;
        }

        System.out.print("Seat: ");
        int[] p = parseSeat(sc.nextLine());
        if (p == null) {
            System.out.println("Invalid seat. Choose rows A-E and seats 1-5.");
            return;
        }
        if (seats[p[0]][p[1]] != null) {
            System.out.println("Seat " + seatName(p[0], p[1]) + " is already booked.");
            return;
        }
        String id = createBooking(name, p[0], p[1]);
        System.out.println("Booking confirmed. ID: " + id + " | " + name + " | Seat " + seatName(p[0], p[1]));
        if (isFull()) System.out.println("Hall is now full.");
    }

    static void cancelBooking(Scanner sc) {
        System.out.print("Booking ID: ");
        String id = sc.nextLine().trim().toUpperCase();
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("Booking ID not found."); return; }

        int[] p = parseSeat(b.seat);
        bookings.remove(id);
        seats[p[0]][p[1]] = null;

        Waiting w = waitList.poll();
        if (w == null) {
            System.out.println("Booking " + id + " cancelled. Seat " + b.seat + " is now free.");
        } else {
            String newId = createBooking(w.name, p[0], p[1]);
            System.out.println("Booking " + id + " cancelled. Seat " + b.seat
                    + " given to " + w.name + " (" + w.type() + ").");
            System.out.println("New booking ID: " + newId + " | " + w.name + " | Seat " + b.seat);
        }
    }

    static void viewWaitingList() {
        if (waitList.isEmpty()) { System.out.println("Waiting list is empty."); return; }
        List<Waiting> list = new ArrayList<>(waitList);
        Collections.sort(list);
        System.out.println("Waiting list:");
        int i = 1;
        for (Waiting w : list)
            System.out.println(i++ + ". " + w.name + " (" + w.type() + ")");
    }

    static void loadData() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] t = line.split(",");
                if (t.length < 2) continue;
                if (t[0].equals("B") && t.length >= 4) {
                    int[] p = parseSeat(t[3]);
                    if (p == null) continue;
                    seats[p[0]][p[1]] = t[1];
                    bookings.put(t[1], new Booking(t[1], t[2], t[3]));
                } else if (t[0].equals("W") && t.length >= 4) {
                    waitList.add(new Waiting(t[1], t[2].equals("Gold"), Long.parseLong(t[3])));
                    arrival = Math.max(arrival, Long.parseLong(t[3]) + 1);
                } else if (t[0].equals("N")) {
                    nextId = Math.max(nextId, Integer.parseInt(t[1]));
                }
            }
            for (String id : bookings.keySet())
                nextId = Math.max(nextId, Integer.parseInt(id.substring(1)) + 1);
        } catch (Exception e) {
            System.out.println("Could not read " + FILE + ": " + e.getMessage());
        }
    }

    static void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            List<String> ids = new ArrayList<>(bookings.keySet());
            Collections.sort(ids);
            for (String id : ids) {
                Booking b = bookings.get(id);
                pw.println("B," + b.id + "," + b.name + "," + b.seat);
            }
            List<Waiting> list = new ArrayList<>(waitList);
            Collections.sort(list);
            for (Waiting w : list)
                pw.println("W," + w.name + "," + w.type() + "," + w.order);
            pw.println("N," + nextId);
            System.out.println("Data saved to " + FILE);
        } catch (IOException e) {
            System.out.println("Could not save data: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        loadData();
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n===== MOVIE TICKET BOOKING =====");
            System.out.println("1. View seats");
            System.out.println("2. Book seat");
            System.out.println("3. Cancel booking");
            System.out.println("4. View waiting list");
            System.out.println("5. Exit");
            System.out.print("Choose option: ");
            if (!sc.hasNextLine()) { saveData(); break; }
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1": viewSeats(); break;
                case "2": bookSeat(sc); break;
                case "3": cancelBooking(sc); break;
                case "4": viewWaitingList(); break;
                case "5": saveData(); System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid option. Choose 1-5.");
            }
        }
    }
}
