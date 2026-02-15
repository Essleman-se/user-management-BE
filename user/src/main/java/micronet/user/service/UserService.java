package micronet.user.service;

import micronet.user.dto.UserPatchDTO;
import micronet.user.dto.UserRequestDTO;
import micronet.user.dto.UserResponseDTO;

import java.util.List;

public interface UserService {

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(Long id);

    UserResponseDTO getUserByEmail(String email);

    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO);

    UserResponseDTO patchUser(Long id, UserPatchDTO userPatchDTO);

    void deleteUser(Long id);
}


