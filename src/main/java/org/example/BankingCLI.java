import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord;
    List<String>  transactions = new ArrayList<>();
}

class Trie {
    private final TrieNode root = new TrieNode();

    public void insert(String transaction) {
        TrieNode node = root;
        for (char c : transaction.toLowerCase().toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEndOfWord = true;
        node.transactions.add(transaction);
    }

    public List<String> search(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toLowerCase().toCharArray()) {
            if (!node.children.containsKey(c)) return Collections.emptyList();
            node = node.children.get(c);
        }
        return collectTransactions(node);
    }

    private List<String> collectTransactions(TrieNode node) {
        List<String> results = new ArrayList<>();
        if (node.isEndOfWord) results.addAll(node.transactions);
        for (TrieNode child : node.children.values()) results.addAll(collectTransactions(child));
        return results;
    }
}

class Transaction {
    public String type;
    public double amount;
    public String description;

    public Transaction() {}

    public Transaction(String type, double amount, String description) {
        this.type = type;
        this.amount = amount;
        this.description = description;
    }
}

class Account {
    public String username;
    public double balance;
    public List<Transaction> transactions = new ArrayList<>();

    public Account() {}

    public Account(String username, double balance) {
        this.username = username;
        this.balance = balance;
    }
}

public class BankingCLI {
    private static final String DATA_FILE = "bank_data.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Scanner scanner = new Scanner(System.in);
    private static Map<String, Account> accounts = new HashMap<>();
    private static Trie transactionTrie = new Trie();

    public static void main(String[] args) {
        loadAccounts();
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        accounts.putIfAbsent(username, new Account(username, 0));

        while (true) {
            System.out.println("\n1. Deposit\n2. Withdraw\n3. Check Balance\n4. Search Transactions\n5. Exit");
            System.out.print("Select an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> deposit(username);
                case 2 -> withdraw(username);
                case 3 -> checkBalance(username);
                case 4 -> searchTransactions();
                case 5 -> {
                    saveAccounts();
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static void deposit(String username) {
        System.out.print("Enter deposit amount: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        if (amount <= 0) {
            System.out.println("Invalid amount.");
            return;
        }

        System.out.println("Enter transaction description: ");
        String description = scanner.nextLine();

        accounts.get(username).balance += amount;
        Transaction transaction = new Transaction("Deposit", amount, description);
        accounts.get(username).transactions.add(transaction);
        transactionTrie.insert(description);
        saveAccounts();
        System.out.println("Deposit successful.");
    }

    private static void withdraw(String username) {
        System.out.print("Enter withdrawal amount: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        if (amount <= 0 || amount > accounts.get(username).balance) {
            System.out.println("Invalid or insufficient balance.");
            return;
        }

        System.out.print("Enter transaction description: ");
        String description = scanner.nextLine();

        accounts.get(username).balance -= amount;
        Transaction transaction = new Transaction("Withdraw", amount, description);
        accounts.get(username).transactions.add(transaction);
        transactionTrie.insert(description);
        saveAccounts();
        System.out.println("Withdrawal successful.");
    }

    private static void checkBalance(String username) {
        System.out.println("Current Balance: $" + accounts.get(username).balance);
    }

    private static void searchTransactions() {
        System.out.print("enter search keyword: ");
        String keyword = scanner.nextLine();
        List<String> results = transactionTrie.search(keyword);
        if (results.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            System.out.println("Matching Transactions:");
            results.forEach(System.out::println);
        }
    }

    private static void loadAccounts() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                accounts = objectMapper.readValue(file, objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Account.class));
                accounts.values().forEach(account -> account.transactions.forEach(transaction -> transactionTrie.insert(transaction.description)));
            }
        } catch (IOException e) {
            System.out.println("Error loading data.");
        }
    }

    private static void saveAccounts() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(DATA_FILE), accounts);
        } catch (IOException e) {
            System.out.println("Error saving data.");
        }
    }
}
