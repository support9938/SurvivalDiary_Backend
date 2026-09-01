package com.survivaldiary.domain.admin.service;

import com.survivaldiary.domain.admin.dto.AdminUpdateUserRequest;
import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private UserRepository userRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        adminService = new AdminService(userRepository, mock(ExpenseRepository.class));
    }

    @Test
    void 회원_상세_정보를_조회한다() {
        User user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var response = adminService.user(1L);

        assertThat(response.email()).isEqualTo("member@example.com");
        assertThat(response.nickname()).isEqualTo("생존러");
        assertThat(response.role()).isEqualTo(User.Role.USER);
    }

    @Test
    void 관리자가_회원_프로필을_수정한다() {
        User user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(
                "변경 이름",
                "변경 닉네임",
                "010-2222-3333",
                LocalDate.of(1998, 3, 15),
                User.Gender.FEMALE,
                "부산광역시",
                "주거, 생활비",
                "수정된 소개"
        );

        var response = adminService.updateUser(1L, request);

        assertThat(response.name()).isEqualTo("변경 이름");
        assertThat(response.nickname()).isEqualTo("변경 닉네임");
        assertThat(response.phone()).isEqualTo("010-2222-3333");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1998, 3, 15));
        assertThat(response.region()).isEqualTo("부산광역시");
        assertThat(user.getBio()).isEqualTo("수정된 소개");
    }

    @Test
    void 없는_회원은_수정할_수_없다() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(
                "이름", null, null, null, null, null, null, null
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminService.updateUser(99L, request)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User user() {
        return User.builder()
                .email("member@example.com")
                .password("encoded-password")
                .name("회원")
                .nickname("생존러")
                .role(User.Role.USER)
                .build();
    }
}
