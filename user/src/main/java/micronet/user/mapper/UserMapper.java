package micronet.user.mapper;

import micronet.user.dto.UserPatchDTO;
import micronet.user.dto.UserRequestDTO;
import micronet.user.dto.UserResponseDTO;
import micronet.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static User toEntity(UserRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setName(dto.getName());
        user.setAge(dto.getAge());
        user.setSex(dto.getSex());
        user.setEmail(dto.getEmail());
        return user;
    }

    public static UserResponseDTO toResponseDTO(User entity) {
        if (entity == null) {
            return null;
        }
        
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAge(entity.getAge());
        dto.setSex(entity.getSex());
        dto.setEmail(entity.getEmail());
        return dto;
    }

    public static List<UserResponseDTO> toResponseDTOList(List<User> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(UserMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Helper method to merge UserPatchDTO into existing User entity (for PATCH)
    public static void mergeUserPatch(User user, UserPatchDTO dto) {
        if (dto == null || user == null) {
            return;
        }
        
        if (dto.getName() != null) {
            user.setName(dto.getName());
        }
        if (dto.getAge() != null) {
            user.setAge(dto.getAge());
        }
        if (dto.getSex() != null) {
            user.setSex(dto.getSex());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
    }
}
