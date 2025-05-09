package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.exception.ItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ItemController.
 */
@WebMvcTest(ItemController.class)
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

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
    void getAllItems_ShouldReturnAllItems() throws Exception {
        when(itemService.findAll()).thenReturn(testItems);

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Test Item")))
                .andExpect(jsonPath("$[1].name", is("Second Item")));

        verify(itemService).findAll();
    }

    @Test
    void getItemById_WhenItemExists_ShouldReturnItem() throws Exception {
        when(itemService.findById(1L)).thenReturn(testItem);

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Item")));

        verify(itemService).findById(1L);
    }

    @Test
    void getItemById_WhenItemDoesNotExist_ShouldReturn404() throws Exception {
        when(itemService.findById(999L)).thenThrow(new ItemNotFoundException(999L));

        mockMvc.perform(get("/api/items/999"))
                .andExpect(status().isNotFound());

        verify(itemService).findById(999L);
    }

    @Test
    void createItem_WithValidItem_ShouldReturnCreatedItem() throws Exception {
        when(itemService.save(any(Item.class))).thenReturn(testItem);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Item")));

        verify(itemService).save(any(Item.class));
    }

    @Test
    void updateItem_WhenItemExists_ShouldReturnUpdatedItem() throws Exception {
        when(itemService.findById(1L)).thenReturn(testItem);
        when(itemService.save(any(Item.class))).thenReturn(testItem);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Item")));

        verify(itemService).findById(1L);
        verify(itemService).save(any(Item.class));
    }

    @Test
    void deleteItem_WhenItemExists_ShouldReturnNoContent() throws Exception {
        when(itemService.findById(1L)).thenReturn(testItem);
        doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());

        verify(itemService).findById(1L);
        verify(itemService).deleteById(1L);
    }

    @Test
    void processItems_WithAsyncDispatch_ShouldReturnProcessedItems() throws Exception {
        List<Item> processedItems = testItems.stream()
                .map(item -> new Item(
                        item.getId(),
                        item.getName(),
                        item.getDescription(),
                        "PROCESSED",
                        item.getEmail()
                ))
                .collect(Collectors.toList());

        CompletableFuture<List<Item>> future = CompletableFuture.completedFuture(processedItems);
        when(itemService.processItemsAsync()).thenReturn(future);

        MvcResult mvcResult = mockMvc.perform(get("/api/items/process"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Test Item")))
                .andExpect(jsonPath("$[0].status", is("PROCESSED")))
                .andExpect(jsonPath("$[1].name", is("Second Item")))
                .andExpect(jsonPath("$[1].status", is("PROCESSED")));

        verify(itemService).processItemsAsync();
    }

}
