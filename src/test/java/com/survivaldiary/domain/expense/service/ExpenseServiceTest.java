package com.survivaldiary.domain.expense.service;

import com.survivaldiary.domain.expense.dto.CreateAutoExpenseRequest;
import com.survivaldiary.domain.expense.entity.Expense;
import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseServiceTest {

    private ExpenseRepository expenseRepository;
    private UserRepository userRepository;
    private ExpenseService service;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ExpenseService(expenseRepository, userRepository);
    }

    @Test
    void 확인한_결제_알림을_AUTO_지출로_저장한다() {
        CreateAutoExpenseRequest request = request("detection-key");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(expenseRepository.findByUserIdAndDetectionKey(1L, "detection-key"))
                .thenReturn(Optional.empty());
        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createAutoExpense(1L, request);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();
        assertThat(saved.getEntryType()).isEqualTo(Expense.EntryType.AUTO);
        assertThat(saved.getNotificationSource()).isEqualTo("토스");
        assertThat(saved.getDetectionKey()).isEqualTo("detection-key");
        assertThat(response.entryType()).isEqualTo(Expense.EntryType.AUTO);
        assertThat(response.notificationSource()).isEqualTo("토스");
    }

    @Test
    void 같은_감지_키가_이미_있으면_기존_지출을_반환한다() {
        Expense existing = Expense.auto(
                1L,
                2L,
                "스타벅스 강남점",
                5500L,
                LocalDateTime.of(2026, 8, 3, 9, 42),
                null,
                "토스",
                "detection-key"
        );
        when(userRepository.existsById(1L)).thenReturn(true);
        when(expenseRepository.findByUserIdAndDetectionKey(1L, "detection-key"))
                .thenReturn(Optional.of(existing));

        var response = service.createAutoExpense(1L, request("detection-key"));

        assertThat(response.entryType()).isEqualTo(Expense.EntryType.AUTO);
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void 다른_사용자의_자동_지출은_저장할_수_없다() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAutoExpense(2L, request("detection-key"))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    private CreateAutoExpenseRequest request(String detectionKey) {
        return new CreateAutoExpenseRequest(
                1L,
                2L,
                "스타벅스 강남점",
                5500,
                LocalDateTime.of(2026, 8, 3, 9, 42),
                null,
                detectionKey,
                "토스"
        );
    }
}
