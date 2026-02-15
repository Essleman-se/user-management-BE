package micronet.user.config;

import micronet.user.model.User;
import micronet.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            // Only initialize if database is empty
            if (userRepository.count() == 0) {
                // Seed data with 5 initial records (1 admin, 4 regular users)
                userRepository.save(createUser("John Doe", 25, "Male", "admin@example.com", "admin123", "ADMIN"));
                userRepository.save(createUser("Jane Smith", 30, "Female", "jane.smith@example.com", "password123", "USER"));
                userRepository.save(createUser("Bob Johnson", 28, "Male", "bob.johnson@example.com", "password123", "USER"));
                userRepository.save(createUser("Alice Williams", 35, "Female", "alice.williams@example.com", "password123", "USER"));
                userRepository.save(createUser("Charlie Brown", 22, "Male", "charlie.brown@example.com", "password123", "USER"));
            }
        };
    }

    private User createUser(String name, Integer age, String sex, String email, String password, String role) {
        User user = new User();
        user.setName(name);
        user.setAge(age);
        user.setSex(sex);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return user;
    }
}
