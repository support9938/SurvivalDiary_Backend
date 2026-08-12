package com.survivaldiary.global.config;

import com.survivaldiary.domain.community.entity.Post;
import com.survivaldiary.domain.community.repository.PostRepository;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import java.util.List;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BootstrapDataInitializer implements CommandLineRunner {
    private static final String FAQ_CATEGORY = "질문";

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:admin@survivaldiary.local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:1234}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        User admin = userRepository.findByEmail(adminEmail).orElseGet(this::createAdmin);
        if (admin.getRole() != User.Role.ADMIN) {
            admin.promoteToAdmin();
            admin = userRepository.save(admin);
        }
        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            admin.updatePassword(passwordEncoder.encode(adminPassword));
            admin = userRepository.save(admin);
        }
        ensureTestUsers();
        seedFaq(admin);
    }

    private void ensureTestUsers() {
        List<TestUserSeed> seeds = List.of(
                new TestUserSeed("user01@survivaldiary.local", "김절약", "절약하는 김씨", "010-2001-0001", LocalDate.of(1998, 3, 14), User.Gender.FEMALE, "서울", "LIVING_COST,FOOD_COST", "식비와 생활비를 꼼꼼하게 관리하는 테스트 사용자"),
                new TestUserSeed("user02@survivaldiary.local", "이알뜰", "알뜰한 이씨", "010-2002-0002", LocalDate.of(1996, 7, 22), User.Gender.MALE, "경기", "BUDGETING,SAVING_INVESTMENT", "예산을 세우고 저축 습관을 만드는 테스트 사용자"),
                new TestUserSeed("user03@survivaldiary.local", "박생활", "생활비 지킴이", "010-2003-0003", LocalDate.of(2000, 11, 5), User.Gender.FEMALE, "부산", "GOVERNMENT_POLICY,BENEFIT", "정책과 지원 혜택을 찾아보는 테스트 사용자")
        );
        for (TestUserSeed seed : seeds) {
            User user = userRepository.findByEmail(seed.email()).orElseGet(() ->
                    userRepository.save(User.builder()
                            .email(seed.email())
                            .password(passwordEncoder.encode("1234"))
                            .name(seed.name())
                            .nickname(seed.nickname())
                            .phone(seed.phone())
                            .birthDate(seed.birthDate())
                            .gender(seed.gender())
                            .region(seed.region())
                            .signupInterest(seed.interests())
                            .bio(seed.bio())
                            .role(User.Role.USER)
                            .build()));
            user.updateBootstrapProfile(seed.name(), seed.nickname(), seed.phone(), seed.birthDate(),
                    seed.gender(), seed.region(), seed.interests(), seed.bio());
            if (!passwordEncoder.matches("1234", user.getPassword())) {
                user.updatePassword(passwordEncoder.encode("1234"));
            }
            userRepository.save(user);
        }
    }

    private record TestUserSeed(String email, String name, String nickname, String phone,
                                LocalDate birthDate, User.Gender gender, String region,
                                String interests, String bio) {}

    private User createAdmin() {
        return userRepository.save(User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .name("생존일기 운영자")
                .nickname("생존일기 운영팀")
                .role(User.Role.ADMIN)
                .build());
    }

    private void seedFaq(User admin) {
        List<FaqSeed> faqs = List.of(
                new FaqSeed("예산을 처음 세울 때 어디서부터 시작하나요?", "지난달 지출을 큰 항목으로 나누고 꼭 필요한 지출과 줄일 수 있는 지출을 구분해 보세요.", "예산관리\n절약방법"),
                new FaqSeed("식비를 줄이면서 건강하게 먹는 방법이 있나요?", "일주일 식단을 먼저 정하고 장보기 목록을 만들어 보세요. 제철 식재료와 집에 있는 재료를 우선 활용해 보세요.", "식비절약\n생활비절약"),
                new FaqSeed("고정비를 줄일 때 가장 먼저 확인할 것은 무엇인가요?", "통신비, 구독 서비스, 보험처럼 매달 자동으로 빠져나가는 항목을 먼저 확인해 보세요.", "고정비절약\n절약습관"),
                new FaqSeed("생존일기 회원 탈퇴는 어떻게 하나요?", "마이페이지 하단의 회원 탈퇴 메뉴에서 본인 확인 후 탈퇴할 수 있어요. 탈퇴하면 저장된 기록을 복구할 수 없으니 신중하게 진행해 주세요.", "서비스이용\n회원탈퇴"),
                new FaqSeed("생존일기에서 어떤 기능을 이용할 수 있나요?", "지출 기록과 예산 관리, 절약 정보 탐색, 정책 추천, 절약 장소 검색, 커뮤니티 활동을 이용할 수 있어요.", "서비스이용\n생존일기"),
                new FaqSeed("내가 작성한 게시글과 댓글은 어디서 확인하나요?", "마이페이지의 Q&A와 댓글 탭에서 내가 작성한 활동을 확인할 수 있어요. 북마크한 글은 북마크 탭에서 다시 볼 수 있습니다.", "서비스이용\n마이페이지")
        );
        for (FaqSeed faq : faqs) {
            if (postRepository.existsByCategoryAndTitle(FAQ_CATEGORY, faq.title())) continue;
            postRepository.save(Post.builder()
                    .user(admin)
                    .category(FAQ_CATEGORY)
                    .title(faq.title())
                    .content(faq.content())
                    .hashtags(faq.hashtags())
                    .imageAlignment("center")
                    .build());
        }
    }

    private record FaqSeed(String title, String content, String hashtags) {}
}
