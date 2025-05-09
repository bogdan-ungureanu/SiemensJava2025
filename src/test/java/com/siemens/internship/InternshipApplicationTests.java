package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Full integration test for the application.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InternshipApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ItemService itemService;

	private Item testItem;

	@BeforeEach
	void setUp() {
		testItem = new Item(null, "Test Item", "Test Description", "Active", "test@example.com");
	}

	@Test
	void createItemTest() throws Exception {
		String itemJson = objectMapper.writeValueAsString(testItem);

		MvcResult result = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value(testItem.getName()))
				.andReturn();

		String responseJson = result.getResponse().getContentAsString();
		Item createdItem = objectMapper.readValue(responseJson, Item.class);
		assertNotNull(createdItem.getId());
	}

	@Test
	void createItemWithInvalidEmailTest() throws Exception {
		testItem.setEmail("invalid-email");
		String itemJson = objectMapper.writeValueAsString(testItem);

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getItemTest() throws Exception {
		Item savedItem = itemService.save(testItem);

		mockMvc.perform(get("/api/items/{id}", savedItem.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(savedItem.getId()))
				.andExpect(jsonPath("$.name").value(savedItem.getName()));
	}

	@Test
	void getAllItemsTest() throws Exception {
		itemService.save(testItem);

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void updateItemTest() throws Exception {
		Item savedItem = itemService.save(testItem);
		savedItem.setName("Updated Name");

		mockMvc.perform(put("/api/items/{id}", savedItem.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(savedItem)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Name"));
	}

	@Test
	void deleteItemTest() throws Exception {
		Item savedItem = itemService.save(testItem);

		mockMvc.perform(delete("/api/items/{id}", savedItem.getId()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/items/{id}", savedItem.getId()))
				.andExpect(status().isNotFound());
	}

	@Test
	void processItemsTest() throws Exception {
		itemService.save(testItem);


		MvcResult mvcResult = mockMvc.perform(get("/api/items/process"))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name", is("Test Item")))
				.andExpect(jsonPath("$[0].status", is("PROCESSED")));
	}


}
