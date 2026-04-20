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
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        return user;
    }

    public static UserResponseDTO toResponseDTO(User entity) {
        if (entity == null) {
            return null;
        }

        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setStatus(entity.getStatus());
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

    public static void mergeUserPatch(User user, UserPatchDTO dto) {
        if (dto == null || user == null) {
            return;
        }

        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
    }
}
