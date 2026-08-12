package com.example.gitprocessor.controller;

import com.example.gitprocessor.model.AppUser;
import com.example.gitprocessor.repository.AppUserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Session-based login backed by APP_USER table in Oracle.
 * On first startup, seeds a default admin user if no users exist.
 */
@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    public static final String SESSION_USER_KEY = "loggedInUser";
    public static final String SESSION_ROLE_KEY = "loggedInRole";
    public static final String SESSION_DISPLAY_KEY = "loggedInDisplay";

    private final AppUserRepository userRepo;

    public LoginController(AppUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /** Seed default admin user on first startup if no users exist */
    @PostConstruct
    public void seedDefaultAdmin() {
        if (userRepo.count() == 0) {
            AppUser admin = new AppUser("admin", "admin123", "Administrator", "ADMIN");
            userRepo.save(admin);
            log.info("Seeded default admin user (admin / admin123). Change password after first login.");
        }
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            HttpSession session,
                            Model model) {
        if (session.getAttribute(SESSION_USER_KEY) != null) {
            return "redirect:/";
        }
        if (error  != null) model.addAttribute("error", "Invalid username or password.");
        if (logout != null) model.addAttribute("loggedOut", true);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        String trimmedUser = username == null ? "" : username.trim();
        String trimmedPass = password == null ? "" : password.trim();

        var optUser = userRepo.findByUsernameAndActiveTrue(trimmedUser);
        if (optUser.isPresent() && optUser.get().getPassword().equals(trimmedPass)) {
            AppUser user = optUser.get();
            session.setAttribute(SESSION_USER_KEY,    user.getUsername());
            session.setAttribute(SESSION_ROLE_KEY,    user.getRole());
            session.setAttribute(SESSION_DISPLAY_KEY, user.getDisplayName() != null
                    ? user.getDisplayName() : user.getUsername());
            return "redirect:/";
        }
        model.addAttribute("error", "Invalid username or password.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    /** REST: returns logged-in user info for JS consumption */
    @GetMapping("/api/session/user")
    @ResponseBody
    public Map<String, String> sessionUser(HttpSession session) {
        String user    = (String) session.getAttribute(SESSION_USER_KEY);
        String role    = (String) session.getAttribute(SESSION_ROLE_KEY);
        String display = (String) session.getAttribute(SESSION_DISPLAY_KEY);
        return Map.of(
                "username",    user    != null ? user    : "",
                "role",        role    != null ? role    : "",
                "displayName", display != null ? display : ""
        );
    }
}