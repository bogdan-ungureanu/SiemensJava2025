package com.siemens.internship;

import com.siemens.internship.exception.ItemNotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        // Original code
        //        for (Long id: itemIds) {
        //            CompletableFuture.runAsync(() -> {
        //                try {
        //                    Thread.sleep(100);
        //
        //                    Item item = itemRepository.findById(id).orElse(null);
        //                    if (item == null) {
        //                        return;
        //                    }
        //
        //                    processedCount++;
        //
        //                    item.setStatus("PROCESSED");
        //                    itemRepository.save(item);
        //                    processedItems.add(item);
        //
        //                } catch (InterruptedException e) {
        //                    System.out.println("Error: " + e.getMessage());
        //                }
        //            }, executor);
        //        }

        // Refactored code
        // Improvements:
        // 1. Use CompletableFuture composition to coordinate multiple async operations
        // 2. Return a CompletableFuture instead of a list of items
        // 3. Throw a new CompletionException if an error occurs during processing
        // 4. Remove the shared state altogether and return the list of processed items directly as a CompletableFuture
        // (we could synchronize the processedCount with AtomicInteger and the processedItems with Collections.synchronizedList,
        // but that would be unnecessary for this app)
        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Item>> futures = itemIds.stream() // use functional style
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(100);

                        return itemRepository.findById(id)
                                .map(item -> {
                                    item.setStatus("PROCESSED");
                                    return itemRepository.save(item);
                                })
                                .orElse(null);
                    } catch (InterruptedException e) {
                        // in case of exceptions, interrupt the thread and propagate the exception
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    }
                }, executor)).toList();

        // wait for all futures to complete and return the list of processed items
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

}

