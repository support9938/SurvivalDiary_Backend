package com.survivaldiary.domain.community.service;

import com.survivaldiary.domain.community.dto.CreatePostRequest;
import com.survivaldiary.domain.community.dto.CommentResponse;
import com.survivaldiary.domain.community.dto.CreateCommentRequest;
import com.survivaldiary.domain.community.entity.Comment;
import com.survivaldiary.domain.community.dto.PostResponse;
import com.survivaldiary.domain.community.entity.Post;
import com.survivaldiary.domain.community.entity.PostBookmark;
import com.survivaldiary.domain.community.repository.PostBookmarkRepository;
import com.survivaldiary.domain.community.repository.PostInteractionRepository;
import com.survivaldiary.domain.community.repository.PostRepository;
import com.survivaldiary.domain.community.repository.CommentRepository;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.domain.savingbadge.dto.SavingBadgeResponse;
import com.survivaldiary.domain.savingbadge.service.SavingBadgeService;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final String SAVING_PROOF_CATEGORY = "절약 인증";
    private static final String INQUIRY_CATEGORY = "질문";
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostBookmarkRepository bookmarkRepository;
    private final PostInteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final SavingBadgeService savingBadgeService;

    @Transactional
    public PostResponse create(Long userId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        boolean commentsDisabled = user.getRole() == User.Role.ADMIN
                && Boolean.TRUE.equals(request.commentsDisabled());
        boolean commentsHidden = user.getRole() == User.Role.ADMIN
                && Boolean.TRUE.equals(request.commentsHidden());
        boolean adminInquiry = INQUIRY_CATEGORY.equals(request.category())
                && Boolean.TRUE.equals(request.adminInquiry());
        Post post = postRepository.save(Post.builder().user(user).category(request.category())
                .title(request.title()).content(request.content())
                .hashtags(join(request.hashtags())).imageUrls(join(request.imageUrls()))
                .imageAlignment(request.imageAlignment())
                .commentsDisabled(commentsDisabled)
                .commentsHidden(commentsHidden)
                .adminInquiry(adminInquiry)
                .secret(Boolean.TRUE.equals(request.secret()))
                .build());
        return response(post, userId);
    }

    @Transactional
    public PostResponse update(Long postId, Long userId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Post post = requireOwner(postId, user);
        boolean commentsDisabled = user.getRole() == User.Role.ADMIN
                && Boolean.TRUE.equals(request.commentsDisabled());
        boolean commentsHidden = user.getRole() == User.Role.ADMIN
                && Boolean.TRUE.equals(request.commentsHidden());
        boolean adminInquiry = INQUIRY_CATEGORY.equals(request.category())
                && Boolean.TRUE.equals(request.adminInquiry());
        post.update(request.category(), request.title(), request.content(), join(request.hashtags()),
                join(request.imageUrls()), request.imageAlignment(), commentsDisabled, commentsHidden,
                adminInquiry, Boolean.TRUE.equals(request.secret()));
        return response(post, userId);
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        postRepository.delete(requireOwner(postId, user));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> list(Long userId, String category, int page, int size) {
        Page<Post> posts = category == null || category.isBlank()
                ? postRepository.findCommunityPosts(User.Role.USER,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                : postRepository.findCommunityPostsByCategory(category, User.Role.USER,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return responses(posts, userId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> popular(Long userId, int size) {
        return responses(postRepository.findPopularCommunityPosts(PageRequest.of(0, size)), userId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> adminCommunityPosts(Long userId, String category, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Post> posts = category == null || category.isBlank()
                ? postRepository.findAllByAdminInquiryFalseOrderByCreatedAtDescIdDesc(pageable)
                : postRepository.findAllByCategoryAndAdminInquiryFalseOrderByCreatedAtDescIdDesc(
                        category.trim(), pageable);
        return responses(posts, userId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> adminInquiryPosts(Long userId, int page, int size) {
        return responses(postRepository.findAllByAdminInquiryTrueOrderByCreatedAtDescIdDesc(
                PageRequest.of(page, size)), userId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> faqs(Long userId, int page, int size) {
        return responses(postRepository.findAllByCategoryAndUserRoleAndAdminInquiryFalseOrderByCreatedAtDescIdDesc(
                INQUIRY_CATEGORY, User.Role.ADMIN, PageRequest.of(page, size)), userId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> myPosts(Long userId, String category, int page, int size) {
        return responses(postRepository.findAllByUserIdAndCategoryOrderByCreatedAtDescIdDesc(
                userId, category, PageRequest.of(page, size)), userId);
    }

    @Transactional(readOnly = true)
    public long unansweredAdminInquiryCount() {
        return postRepository.countUnansweredAdminInquiries(User.Role.ADMIN);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        requireAccessible(post, userId);
        return response(post, userId);
    }

    private Post requireOwner(Long postId, User user) {
        Post post = requirePost(postId);
        if (user.getRole() != User.Role.ADMIN && !post.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return post;
    }

    @Transactional
    public PostResponse toggleLike(Long postId, Long userId) {
        requireAccessible(requirePost(postId), userId);
        interactionRepository.toggleLike(postId, userId);
        return get(postId, userId);
    }

    @Transactional
    public PostResponse toggleBookmark(Long postId, Long userId) {
        Post post = requirePost(postId);
        requireAccessible(post, userId);
        if (bookmarkRepository.existsByPostIdAndUserId(postId, userId)) {
            bookmarkRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            bookmarkRepository.save(new PostBookmark(post, user));
        }
        return get(postId, userId);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long postId, Long userId) {
        requireAccessible(requirePost(postId), userId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(comment -> CommentResponse.from(comment, userId))
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CreateCommentRequest request) {
        Post post = requirePost(postId);
        requireAccessible(post, userId);
        if (post.isCommentsDisabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Comment comment = commentRepository.save(new Comment(post, user, request.content().trim()));
        return CommentResponse.from(comment, userId);
    }

    @Transactional
    public CommentResponse createAdminAnswer(Long postId, Long userId, CreateCommentRequest request) {
        User admin = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (admin.getRole() != User.Role.ADMIN) throw new BusinessException(ErrorCode.FORBIDDEN);
        Post post = requirePost(postId);
        if (!post.isAdminInquiry()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Comment comment = commentRepository.save(new Comment(post, admin, request.content().trim()));
        return CommentResponse.from(comment, userId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() != User.Role.ADMIN && !comment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentRepository.delete(comment);
    }

    private Post requirePost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private PostResponse response(Post post, Long userId) {
        SavingBadgeResponse badge = SAVING_PROOF_CATEGORY.equals(post.getCategory())
                ? savingBadgeService.badgeFor(post.getUser().getId())
                : null;
        return response(post, userId, badge, isAdmin(userId));
    }

    private void requireAccessible(Post post, Long userId) {
        if (!canAccess(post, userId, isAdmin(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean canAccess(Post post, Long userId, boolean viewerIsAdmin) {
        return (!post.isAdminInquiry() && !post.isSecret())
                || post.getUser().getId().equals(userId)
                || viewerIsAdmin;
    }

    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == User.Role.ADMIN)
                .orElse(false);
    }

    private Page<PostResponse> responses(Page<Post> posts, Long userId) {
        Set<Long> authorIds = posts.stream()
                .filter(post -> SAVING_PROOF_CATEGORY.equals(post.getCategory()))
                .map(post -> post.getUser().getId())
                .collect(Collectors.toSet());
        Map<Long, SavingBadgeResponse> badges = savingBadgeService.badgesFor(authorIds);
        boolean viewerIsAdmin = isAdmin(userId);
        return posts.map(post -> response(
                post,
                userId,
                SAVING_PROOF_CATEGORY.equals(post.getCategory())
                        ? badges.get(post.getUser().getId())
                        : null,
                viewerIsAdmin
        ));
    }

    private PostResponse response(Post post, Long userId, SavingBadgeResponse authorSavingBadge,
                                  boolean viewerIsAdmin) {
        boolean accessible = canAccess(post, userId, viewerIsAdmin);
        boolean answered = post.isAdminInquiry()
                && commentRepository.existsByPostIdAndUserRole(post.getId(), User.Role.ADMIN);
        return PostResponse.from(post, interactionRepository.likeCount(post.getId()),
                interactionRepository.commentCount(post.getId()), bookmarkRepository.countByPostId(post.getId()),
                interactionRepository.likedBy(post.getId(), userId),
                bookmarkRepository.existsByPostIdAndUserId(post.getId(), userId),
                post.getUser().getId().equals(userId), accessible, answered, authorSavingBadge);
    }

    private static String join(List<String> values) {
        if (values == null) return null;
        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(String::trim).reduce((a, b) -> a + "\n" + b).orElse(null);
    }
}
