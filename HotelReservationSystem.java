import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

class Room {
    int id;
    String number;
    String category;
    double price;

    public Room(int id, String number, String category, double price) {
        this.id = id;
        this.number = number;
        this.category = category;
        this.price = price;
    }

    @Override
    public String toString() {
        return id + "," + number + "," + category + "," + price;
    }
}

class Reservation {
    int id;
    int roomId;
    String guest;
    LocalDate checkIn;
    LocalDate checkOut;
    double total;
    String status;
    String paymentRef;

    public Reservation(int id, int roomId, String guest, LocalDate checkIn, LocalDate checkOut,
                       double total, String status, String paymentRef) {
        this.id = id;
        this.roomId = roomId;
        this.guest = guest;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.total = total;
        this.status = status;
        this.paymentRef = paymentRef;
    }

    @Override
    public String toString() {
        return id + "," + roomId + "," + guest + "," + checkIn + "," + checkOut + "," +
                total + "," + status + "," + paymentRef;
    }
}

class FileDB {
    private final String roomFile = "rooms.csv";
    private final String reservationFile = "reservations.csv";
    private DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Load rooms
    public List<Room> loadRooms() {
        List<Room> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(roomFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                list.add(new Room(Integer.parseInt(p[0]), p[1], p[2], Double.parseDouble(p[3])));
            }
        } catch (IOException e) {
            // ignore if file doesn't exist
        }
        return list;
    }

    // Save rooms
    public void saveRooms(List<Room> rooms) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(roomFile))) {
            for (Room r : rooms) {
                pw.println(r.toString());
            }
        }
    }

    // Load reservations
    public List<Reservation> loadReservations() {
        List<Reservation> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                list.add(new Reservation(
                        Integer.parseInt(p[0]),
                        Integer.parseInt(p[1]),
                        p[2],
                        LocalDate.parse(p[3]),
                        LocalDate.parse(p[4]),
                        Double.parseDouble(p[5]),
                        p[6],
                        p[7]
                ));
            }
        } catch (IOException e) {
            // ignore
        }
        return list;
    }

    // Save reservations
    public void saveReservations(List<Reservation> reservations) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(reservationFile))) {
            for (Reservation r : reservations) {
                pw.println(r.toString());
            }
        }
    }
}

class Payment {
    public static String pay(double amount, String card) {
        if (card.endsWith("0") || card.endsWith("2") || card.endsWith("4") ||
            card.endsWith("6") || card.endsWith("8")) {
            return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return null; // fail
    }
}

public class HotelReservationSystem {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        FileDB db = new FileDB();

        List<Room> rooms = db.loadRooms();
        List<Reservation> reservations = db.loadReservations();

        // Seed rooms if empty
        if (rooms.isEmpty()) {
            rooms.add(new Room(1, "101", "Standard", 60));
            rooms.add(new Room(2, "201", "Deluxe", 100));
            rooms.add(new Room(3, "301", "Suite", 150));
            db.saveRooms(rooms);
        }

        int nextResId = reservations.stream().mapToInt(r -> r.id).max().orElse(0) + 1;

        while (true) {
            System.out.println("\n1) Search rooms\n2) Book room\n3) Cancel reservation\n4) View reservation\n5) List reservations\n0) Exit");
            System.out.print("Choice: ");
            int ch = Integer.parseInt(sc.nextLine());

            if (ch == 1) {
                System.out.print("Category (blank=all): ");
                String cat = sc.nextLine();
                for (Room r : rooms) {
                    if (cat.isEmpty() || r.category.equalsIgnoreCase(cat)) {
                        System.out.println(r.id + " | Room " + r.number + " | " + r.category + " | $" + r.price);
                    }
                }
            } else if (ch == 2) {
                System.out.print("Room ID: ");
                int rid = Integer.parseInt(sc.nextLine());
                Room room = rooms.stream().filter(r -> r.id == rid).findFirst().orElse(null);
                if (room == null) {
                    System.out.println("Room not found.");
                    continue;
                }
                System.out.print("Guest name: ");
                String guest = sc.nextLine();
                System.out.print("Check-in (yyyy-mm-dd): ");
                LocalDate in = LocalDate.parse(sc.nextLine());
                System.out.print("Check-out (yyyy-mm-dd): ");
                LocalDate out = LocalDate.parse(sc.nextLine());

                // Check availability
                boolean available = reservations.stream().noneMatch(r ->
                        r.roomId == rid && !(r.checkOut.isBefore(in) || r.checkIn.isAfter(out))
                );
                if (!available) {
                    System.out.println("Room not available!");
                    continue;
                }

                long nights = java.time.temporal.ChronoUnit.DAYS.between(in, out);
                double total = nights * room.price;
                System.out.print("Enter card number: ");
                String card = sc.nextLine();
                String ref = Payment.pay(total, card);
                if (ref == null) {
                    System.out.println("Payment failed!");
                } else {
                    Reservation res = new Reservation(nextResId++, rid, guest, in, out, total, "PAID", ref);
                    reservations.add(res);
                    db.saveReservations(reservations);
                    System.out.println("Reservation confirmed. ID=" + res.id + " Ref=" + ref + " Total=$" + total);
                }
            } else if (ch == 3) {
                System.out.print("Reservation ID to cancel: ");
                int id = Integer.parseInt(sc.nextLine());
                boolean removed = reservations.removeIf(r -> r.id == id);
                if (removed) {
                    db.saveReservations(reservations);
                    System.out.println("Cancelled.");
                } else {
                    System.out.println("Not found.");
                }
            } else if (ch == 4) {
                System.out.print("Reservation ID: ");
                int id = Integer.parseInt(sc.nextLine());
                Reservation r = reservations.stream().filter(x -> x.id == id).findFirst().orElse(null);
                if (r == null) {
                    System.out.println("Not found.");
                } else {
                    Room room = rooms.stream().filter(x -> x.id == r.roomId).findFirst().orElse(null);
                    System.out.println("Reservation " + r.id + " Guest=" + r.guest +
                            " Room=" + (room != null ? room.number : "?") +
                            " (" + (room != null ? room.category : "?") + ")" +
                            " " + r.checkIn + "->" + r.checkOut +
                            " $" + r.total + " Status=" + r.status + " Ref=" + r.paymentRef);
                }
            } else if (ch == 5) {
                for (Reservation r : reservations) {
                    Room room = rooms.stream().filter(x -> x.id == r.roomId).findFirst().orElse(null);
                    System.out.println("[" + r.id + "] " + r.guest + " - " +
                            (room != null ? room.number : "?") +
                            " " + r.checkIn + "->" + r.checkOut +
                            " $" + r.total + " " + r.status);
                }
            } else if (ch == 0) {
                break;
            }
        }
    }
}
