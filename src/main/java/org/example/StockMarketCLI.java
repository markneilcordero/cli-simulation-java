import java.util.*;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class Order {
    @JsonProperty
    String stockSymbol;
    @JsonProperty
    double price;
    @JsonProperty
    int quantity;
    @JsonProperty
    boolean isBuyOrder;

    public Order(String stockSymbol, double price, int quantity, boolean isBuyOrder) {
        this.stockSymbol = stockSymbol;
        this.price = price;
        this.quantity = quantity;
        this.isBuyOrder = isBuyOrder;
    }

    @Override
    public String toString() {
        return (isBuyOrder ? "BUY" : "SELL ") + quantity + " shares of " + stockSymbol + " at $" + price;
    }
}

class OrderBook {
    private PriorityQueue<Order> buyOrders;
    private PriorityQueue<Order> sellOrders;
    private List<String> tradeHistory;
    private ObjectMapper objectMapper;

    public OrderBook() {
        buyOrders = new PriorityQueue<>((a, b) -> Double.compare(b.price, a.price));
        sellOrders = new PriorityQueue<>(Comparator.comparingDouble(a -> a.price));
        tradeHistory = new ArrayList<>();
        objectMapper = new ObjectMapper();
    }

    public void placeOrder(String stockSymbol, double price, int quantity, boolean isBuyOrder) {
        Order newOrder = new Order(stockSymbol, price, quantity, isBuyOrder);

        if (isBuyOrder) {
            matchOrders(newOrder, sellOrders, buyOrders);
            System.out.println("Buy order placed: " + newOrder.quantity + " shares of " + newOrder.stockSymbol + " at " + newOrder.price);
        } else {
            matchOrders(newOrder, buyOrders, sellOrders);
            System.out.println("Sell order placed: " + newOrder.quantity + " shares of " + newOrder.stockSymbol + " at " + newOrder.price);
        }
    }

    private void matchOrders(Order newOrder, PriorityQueue<Order> oppositeOrders, PriorityQueue<Order> sameOrders) {
        while (!oppositeOrders.isEmpty()) {
            Order bestMatch = oppositeOrders.peek();

            if ((newOrder.isBuyOrder && newOrder.price >= bestMatch.price) ||
                    (!newOrder.isBuyOrder && newOrder.price <= bestMatch.price)) {
                int tradedQuantity = Math.min(newOrder.quantity, bestMatch.quantity);
                tradeHistory.add("TRADE: " + tradedQuantity + " shares of " + newOrder.stockSymbol +
                        " at $" + bestMatch.price);

                newOrder.quantity -= tradedQuantity;
                bestMatch.quantity -= tradedQuantity;

                if (bestMatch.quantity == 0) {
                    oppositeOrders.poll();
                }

                if (newOrder.quantity == 0) {
                    return;
                }
            } else {
                break;
            }
        }

        sameOrders.add(newOrder);
    }

    public void viewOrderBook() {
        System.out.println("\n--- BUY ORDERS ---");
        buyOrders.forEach(System.out::println);
        System.out.println("\n--- SELL ORDERS ---");
        sellOrders.forEach(System.out::println);
    }

    public void viewTradeHistory() {
        System.out.println("\n--- TRADE HISTORY ---");
        tradeHistory.forEach(System.out::println);
    }

    public void saveData(String filename) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("buyOrders", new ArrayList<>(buyOrders));
            data.put("sellOrders", new ArrayList<>(sellOrders));
            data.put("tradeHistory", tradeHistory);

            // Ensure atomic write (write to temp file first)
            File tempFile = new File(filename + ".tmp");
            objectMapper.writeValue(tempFile, data);

            // Rename temp file to actual file
            File actualFile = new File(filename);
            if (actualFile.exists()) actualFile.delete(); // Delete old file
            tempFile.renameTo(actualFile); // Move temp to actual

            System.out.println("✅ Data saved successfully.");
        } catch (IOException e) {
            System.out.println("⚠️ Error saving data.");
            e.printStackTrace();
        }
    }


    public void loadData(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists() || file.length() == 0) {
                System.out.println("No previous data found. Starting fresh.");
                return;
            }

            // Read JSON file into a Map
            Map<String, Object> data = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});

            // Deserialize buy and sell orders
            List<Order> buyOrdersList = objectMapper.convertValue(data.get("buyOrders"), new TypeReference<List<Order>>() {});
            List<Order> sellOrdersList = objectMapper.convertValue(data.get("sellOrders"), new TypeReference<List<Order>>() {});

            buyOrders = new PriorityQueue<>((a, b) -> Double.compare(b.price, a.price));
            sellOrders = new PriorityQueue<>(Comparator.comparingDouble(a -> a.price));

            if (buyOrdersList != null) buyOrders.addAll(buyOrdersList);
            if (sellOrdersList != null) sellOrders.addAll(sellOrdersList);

            tradeHistory = objectMapper.convertValue(data.get("tradeHistory"), new TypeReference<List<String>>() {});

            System.out.println("Data loaded successfully.");
        } catch (IOException e) {
            System.out.println("⚠️ Error loading data. The JSON file might be corrupted. Starting fresh.");
        }
    }

}

public class StockMarketCLI {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        OrderBook orderBook = new OrderBook();
        String filename = "stock_orders.json";
        orderBook.loadData(filename);

        while (true) {
            System.out.println("\nStock Market Order Book");
            System.out.println("1. Place Buy Order");
            System.out.println("2. Place Sell Order");
            System.out.println("3. View Order Book");
            System.out.println("4. View Trade History");
            System.out.println("5. Save & Exit");
            System.out.println("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1, 2 -> {
                    System.out.println("Enter stock symbol: ");
                    String symbol = scanner.nextLine();
                    System.out.println("Enter price: ");
                    double price = scanner.nextDouble();
                    System.out.println("Enter quantity: ");
                    int quantity = scanner.nextInt();
                    boolean isBuyOrder = (choice == 1);
                    orderBook.placeOrder(symbol, price, quantity, isBuyOrder);
                }
                case 3 -> orderBook.viewOrderBook();
                case 4 -> orderBook.viewTradeHistory();
                case 5 -> {
                    orderBook.saveData(filename);
                    System.out.println("Exiting...");
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }
}