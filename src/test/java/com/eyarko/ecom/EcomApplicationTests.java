package com.eyarko.ecom;

import com.eyarko.ecom.controller.AuthController;
import com.eyarko.ecom.controller.CategoryController;
import com.eyarko.ecom.controller.InventoryController;
import com.eyarko.ecom.controller.OrderController;
import com.eyarko.ecom.controller.ProductController;
import com.eyarko.ecom.controller.ReviewController;
import com.eyarko.ecom.controller.UserController;
import com.eyarko.ecom.graphql.MutationController;
import com.eyarko.ecom.graphql.QueryController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EcomApplicationTests {
	@Autowired
	private AuthController authController;

	@Autowired
	private CategoryController categoryController;

	@Autowired
	private InventoryController inventoryController;

	@Autowired
	private OrderController orderController;

	@Autowired
	private ProductController productController;

	@Autowired
	private ReviewController reviewController;

	@Autowired
	private UserController userController;

	@Autowired
	private MutationController mutationController;

	@Autowired
	private QueryController queryController;

	@Test
	void contextLoads() {
		assertNotNull(authController);
		assertNotNull(categoryController);
		assertNotNull(inventoryController);
		assertNotNull(orderController);
		assertNotNull(productController);
		assertNotNull(reviewController);
		assertNotNull(userController);
		assertNotNull(mutationController);
		assertNotNull(queryController);
	}

}
