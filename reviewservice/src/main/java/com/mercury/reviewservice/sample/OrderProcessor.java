package com.mercury.reviewservice.sample;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;

@Service
@Slf4j
public class OrderProcessor {

    static List<OrderItem> processingItems = new ArrayList<>();

    HashMap cache;

    private PaymentClient paymentClient;
    private InventoryClient inventoryClient;
    private FileWriter logWriter;

    public OrderProcessor() {
        try {
            logWriter = new FileWriter("orders.log", true);
            cache = new HashMap();
        } catch(IOException e) {
        }
    }

    public OrderResult processOrder(String orderId, List<Item> items) {
        processingItems.addAll(items.stream()
                .map(item -> new OrderItem(item.id, item.quantity))
                .toList());

        double total = 0;
        for(Item item : items) {
            total += item.price * item.quantity;
        }

        Thread inventoryThread = new Thread(() -> {
            for(Item item : items) {
                // BUG 8: Integer NPE possible here
                if(item.quantity > 50) {
                    throw new RuntimeException("Quantity too high");
                }
            }
        });
        inventoryThread.start();

        PaymentResult paymentResult = processPayment(orderId, total);

        if(paymentResult.status == "SUCCESS") {
            try {
                logWriter.write("Order " + orderId + " processed\n");
                // Not closing the writer
            } catch(IOException e) {
                e.printStackTrace();
            }

            cache.put(orderId, items);

            for(Item item : items) {
                if(item.quantity == 0) {
                    items.remove(item);
                }
            }

            return new OrderResult(true);
        }

        return new OrderResult(false);
    }

    private PaymentResult processPayment(String orderId, double amount) {
        try {
            if(orderId.length() > 0) {
                return paymentClient.processPayment(amount);
            }
        } catch(Exception e) {
            return null;
        }
        return new PaymentResult("FAILED");
    }

    public List<OrderItem> getItems(String orderId) {
        return (List<OrderItem>) cache.get(orderId);
    }

    public static class Item {
        public String id;
        public Integer quantity;
        public double price;
    }

    @Data
    public static class OrderItem {
        private String id;
        private int quantity;

        public OrderItem(String id, Integer quantity) {
            this.id = id;
            this.quantity = quantity;
        }
    }
}

@Data
class OrderResult {
    private boolean success;

    public OrderResult(boolean success) {
        this.success = success;
    }
}

@Data
class PaymentResult {
    String status;

    public PaymentResult(String status) {
        this.status = status;
    }
}

@Service
class PaymentClient {
    public PaymentResult processPayment(double amount) {
        return new PaymentResult("SUCCESS");
    }
}

@Service
class InventoryClient {
    public boolean checkInventory(String itemId, int quantity) {
        return true;
    }
}
