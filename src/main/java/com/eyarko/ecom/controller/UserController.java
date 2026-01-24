package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.UserCreateRequest;
import com.eyarko.ecom.dto.UserResponse;
import com.eyarko.ecom.dto.UserUpdateRequest;
import com.eyarko.ecom.service.UserService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user.
     *
     * @param request user payload
     * @return created user
     */
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseUtil.success("User created", userService.createUser(request));
    }

    /**
     * Retrieves a user by id.
     *
     * @param id user id
     * @return user details
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ResponseUtil.success("User retrieved", userService.getUser(id));
    }

    /**
     * Updates an existing user.
     *
     * @param id user id
     * @param request user payload
     * @return updated user
     */
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseUtil.success("User updated", userService.updateUser(id, request));
    }

    /**
     * Lists all users.
     *
     * @return list of users
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers() {
        return ResponseUtil.success("Users retrieved", userService.listUsers());
    }

    /**
     * Deletes a user by id.
     *
     * @param id user id
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseUtil.success("User deleted", null);
    }
}

