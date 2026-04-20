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
            if (userRepository.count() == 0) {
                userRepository.save(createUser("John", "Doe", "+1555000001", "admin@example.com", "admin123", "ADMIN"));
                userRepository.save(createUser("Jane", "Smith", "+1555000002", "jane.smith@example.com", "password123", "USER"));
                userRepository.save(createUser("Bob", "Johnson", "+1555000003", "bob.johnson@example.com", "password123", "USER"));
                userRepository.save(createUser("Alice", "Williams", "+1555000004", "alice.williams@example.com", "password123", "USER"));
                userRepository.save(createUser("Charlie", "Brown", "+1555000005", "charlie.brown@example.com", "password123", "USER"));
            }
        };
    }

    private User createUser(String firstName, String lastName, String phone, String email, String password, String role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus("ACTIVE");
        return user;
    }
}
