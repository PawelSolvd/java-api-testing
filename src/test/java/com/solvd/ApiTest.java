package com.solvd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvd.model.User;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/*
TODO
 - 100% independent (all resources used in the test should be created as part of the same test)
 - Request body data (or data for response’s body validation) should be stored as a template, do not copy entire requests bodies just adding small changes.
 - cleanup & add utility methods
 */

public class ApiTest {
    private final String endpoint = "https://gorest.co.in/public/v2/users";
    private final String token = "Bearer 9a254e49a66b1afca3271be77ac108c7cd981a2782182ffd221186df0c1d67c7";
    private final URI uri = URI.create(endpoint);

    private List<Long> createdIds = new ArrayList<>();

    @Test
    public void testGetUsers() {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(uri).GET().build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            assertEquals(response.statusCode(), 200);
            assertEquals(response.headers().firstValue("Content-Type").orElse(""), "application/json; charset=utf-8");
            assertTrue(response.headers().firstValue("x-links-current").orElse("").startsWith(endpoint));

            // verify body has correctly formated data
            new ObjectMapper().readValue(response.body(), new TypeReference<List<User>>() {
            }).forEach(u -> assertTrue(u.isValid()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @DataProvider(name = "userIds")
    public Object[] userIdsDataProvider() {
        return new Object[]{
                7395700,
                7395699,
                7395698
        };
    }

    @Test(dataProvider = "userIds")
    public void testGetUser(long id) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + id)).GET().build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            assertEquals(response.statusCode(), 200);
            assertEquals(response.headers().firstValue("Content-Type").orElse(""), "application/json; charset=utf-8");

            // verify body has correctly formated data
            User user = new ObjectMapper().readValue(response.body(), User.class);

            System.out.println(user);
            assertTrue(user.isValid() && user.getId() == id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetMissingUser() {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + "0"))
                    .GET()
                    .header("Authorization", token)
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 404);

            assertEquals(response.body(), "{\"message\":\"Resource not found\"}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @DataProvider(name = "validUsers")
    public Object[] validUsersDataProvider() {
        return new Object[]{
                new User("adam111", "adam44@brakus.test", "male", "active"),
                new User("adam222", "adam55@brakus.test", "male", "active"),
                new User("adam333", "adam66@brakus.test", "male", "active")
        };
    }

    @Test(dataProvider = "validUsers")
    public void testPostUser(User user) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(user)))
                    .header("Authorization", token)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println(new ObjectMapper().writeValueAsString(user));
            assertEquals(response.statusCode(), 201, response.body());
            assertEquals(response.headers().firstValue("Content-Type").orElse(""), "application/json; charset=utf-8");

            User createdUser = new ObjectMapper().readValue(response.body(), User.class);
            assertTrue(user.dataEquals(createdUser));


            HttpRequest getUserRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + createdUser.getId())).GET()
                    .header("Authorization", token)
                    .build();

            var getUserResponse = httpClient.send(getUserRequest, HttpResponse.BodyHandlers.ofString());
            assertTrue(createdUser.dataEquals(new ObjectMapper().readValue(getUserResponse.body(), User.class)));

            createdIds.add(createdUser.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPostUserUnauthorized() {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(new User())))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            assertEquals(response.statusCode(), 401, response.body());
            assertEquals(response.headers().firstValue("Content-Type").orElse(""), "application/json; charset=utf-8");

            assertEquals(new ObjectMapper().readTree(response.body()).path("message").textValue(), "Authentication failed");

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @DataProvider(name = "notValidUsers")
    public Object[] notValidUsersDataProvider() {
        return new Object[][]{
                // email taken
                {new User("a", "gita_johar@grady.test", "male", "active"),
                        "[{\"field\":\"email\",\"message\":\"has already been taken\"}]"},
                // no name
                {new User("", "adam55@brakus.test", "male", "active"),
                        "[{\"field\":\"name\",\"message\":\"can't be blank\"}]"},
                // no email
                {new User("adam", "", "male", "active"),
                        "[{\"field\":\"email\",\"message\":\"can't be blank\"}]"},
                // no gender
                {new User("adam333", "adam66@brakus.test", "", "active"),
                        "[{\"field\":\"gender\",\"message\":\"can't be blank, can be male of female\"}]"},
                // no status
                {new User("adam333", "adam66@brakus.test", "male", ""),
                        "[{\"field\":\"status\",\"message\":\"can't be blank\"}]"},
                // invalid status
                {new User("adam333", "adam66@brakus.test", "male", "off"),
                        "[{\"field\":\"status\",\"message\":\"can't be blank\"}]"},
                // invalid gender
                {new User("adam333", "adam66@brakus.test", "m", "active"),
                        "[{\"field\":\"gender\",\"message\":\"can't be blank, can be male of female\"}]"},
                // empty
                {new User(),
                        "[{\"field\":\"email\",\"message\":\"can't be blank\"},{\"field\":\"name\",\"message\":\"can't be blank\"},{\"field\":\"gender\",\"message\":\"can't be blank, can be male of female\"},{\"field\":\"status\",\"message\":\"can't be blank\"}]"}
        };
    }

    @Test(dataProvider = "notValidUsers")
    public void testPostNotValidUser(User user, String body) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(user)))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", token)
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            assertEquals(response.statusCode(), 422, response.body());
            assertEquals(response.headers().firstValue("Content-Type").orElse(""), "application/json; charset=utf-8");

            assertEquals(response.body(), body);

            System.out.println(response.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @DataProvider(name = "createdUserIds")
    public Object[] createdUserIdsDataProvider() {
        return createdIds.toArray();
    }

    @Test(dataProvider = "createdUserIds", dependsOnMethods = "testPostUser")
    public void testPatchUser(long id) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + id))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"name\":\"ADAM\"}"))
                    .header("Authorization", token)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 200);

            User patchedUser = new ObjectMapper().readValue(response.body(), User.class);
            System.out.println(patchedUser);

            HttpRequest getUserRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + id)).GET()
                    .header("Authorization", token)
                    .build();

            var getUserResponse = httpClient.send(getUserRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(getUserResponse.statusCode(), 200);
            assertEquals(getUserResponse.headers().firstValue("Content-Type").orElse(""), "application/json; charset=utf-8");

            System.out.println(new ObjectMapper().readValue(getUserResponse.body(), User.class));

            assertTrue(new ObjectMapper().readValue(getUserResponse.body(), User.class).dataEquals(patchedUser));
            assertEquals(new ObjectMapper().readValue(getUserResponse.body(), User.class).getName(), "ADAM");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPatchMissingUser() {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + "0"))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"name\":\"ADAM\"}"))
                    .header("Authorization", token)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 404);

            assertEquals(response.body(), "{\"message\":\"Resource not found\"}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(dataProvider = "createdUserIds", dependsOnMethods = "testPatchUser")
    public void testDeleteUser(long id) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + id)).DELETE()
                    .header("Authorization", token)
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 204);

            HttpRequest getUserRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + id)).GET()
                    .header("Authorization", token)
                    .build();

            var getUserResponse = httpClient.send(getUserRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(getUserResponse.statusCode(), 404);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeleteMissingUser() {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint + "/" + "0")).DELETE()
                    .header("Authorization", token)
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(response.statusCode(), 404);

            assertEquals(response.body(), "{\"message\":\"Resource not found\"}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
