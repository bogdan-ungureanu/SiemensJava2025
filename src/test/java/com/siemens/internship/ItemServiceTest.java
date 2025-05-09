package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import static org.mockito.Mockito.*;

/**
 * Unit tests for ItemService.
 */
@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private List<Item> testItems;

    @BeforeEach
    void setUp() {
        testItem = new Item(1L, "Test Item", "Description", "NEW", "test@email.com");
        testItems = Arrays.asList(
                testItem,
                new Item(2L, "Second Item", "Description 2", "NEW", "test2@email.com")
        );
    }

    @Test
    void findAll_ShouldReturnAllItems() {
        when(itemRepository.findAll()).thenReturn(testItems);

        List<Item> result = itemService.findAll();

        assertEquals(2, result.size());
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void findById_WhenItemExists_ShouldReturnItem() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        Item result = itemService.findById(1L);

        assertEquals(testItem.getName(), result.getName());
    }

    @Test
    void save_ShouldReturnSavedItem() {
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        Item result = itemService.save(testItem);

        assertNotNull(result);
        assertEquals(testItem.getName(), result.getName());
    }

    @Test
    void deleteById_ShouldCallRepository() {
        itemService.deleteById(1L);

        verify(itemRepository, times(1)).deleteById(1L);
    }

    @Test
    void processItemsAsync_ShouldProcessItems() throws Exception {
        List<Long> ids = Arrays.asList(1L, 2L);
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("PROCESSED", result.get(0).getStatus());
        assertEquals("PROCESSED", result.get(1).getStatus());
        verify(itemRepository, times(1)).findAllIds();
        verify(itemRepository, times(2)).findById(anyLong());
        verify(itemRepository, times(2)).save(any(Item.class));
    }

    @Test
    void processLargeNumberOfItemsAsync_ShouldCompleteInTime() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            ids.add(i);
        }
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            CompletableFuture<List<Item>> future = itemService.processItemsAsync();
            List<Item> result = future.get();

            assertNotNull(result);
            assertEquals(1000, result.size());
            assertTrue(result.stream().allMatch(item -> "PROCESSED".equals(item.getStatus())));
            verify(itemRepository, times(1)).findAllIds();
            verify(itemRepository, times(1000)).findById(anyLong());
            verify(itemRepository, times(1000)).save(any(Item.class));
        });
    }
}
