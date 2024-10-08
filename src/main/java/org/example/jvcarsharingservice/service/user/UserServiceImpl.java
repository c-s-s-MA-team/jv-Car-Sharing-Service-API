package org.example.jvcarsharingservice.service.user;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.jvcarsharingservice.dto.user.UpdateUserRequestDto;
import org.example.jvcarsharingservice.dto.user.UserDto;
import org.example.jvcarsharingservice.dto.user.registration.RegisterRequestDto;
import org.example.jvcarsharingservice.exception.RegistrationException;
import org.example.jvcarsharingservice.mapper.UserMapper;
import org.example.jvcarsharingservice.model.classes.User;
import org.example.jvcarsharingservice.model.enums.Role;
import org.example.jvcarsharingservice.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    public static final Role ROLE = Role.CUSTOMER;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto register(RegisterRequestDto requestDto) {
        if (userRepository.findUserByEmail(requestDto.getEmail()).isPresent()) {
            throw new RegistrationException("User with this email: "
                    + requestDto.getEmail() + " already exist");
        }
        User user = registerNewUser(requestDto);
        return userMapper.toDto(
                userRepository.save(user));
    }

    @Transactional
    protected User registerNewUser(RegisterRequestDto requestDto) {
        User user = userMapper.toEntity(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setRole(ROLE);
        return user;
    }

    @Override
    @Transactional
    public UserDto updateRole(Long id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("user with this id does not exist")
        );
        switch (user.getRole()) {
            case CUSTOMER -> user.setRole(Role.MANAGER);
            default -> user.setRole(Role.CUSTOMER);
        }
        return userMapper.toDto(
                userRepository.save(user)
        );
    }

    @Override
    public UserDto getMyProfile(User user) {
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateMyProfile(User user, UpdateUserRequestDto requestDto) {
        user.setFirstName(requestDto.firstName());
        user.setLastName(requestDto.lastName());
        return userMapper.toDto(
                userRepository.save(user)
        );
    }
}
