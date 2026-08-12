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
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostBookmarkRepository bookmarkRepository;
    private final PostInteractionRepository interactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostResponse create(Long userId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        boolean commentsDisabled = user.getRole() == User.Role.ADMIN && request.commentsDisabled();
        boolean commentsHidden = user.getRole() == User.Role.ADMIN && request.commentsHidden();
        Post post = postRepository.save(Post.builder().user(user).category(request.category())
                .title(request.title()).content(request.content())
                .hashtags(join(request.hashtags())).imageUrls(join(request.imageUrls()))
                .imageAlignment(request.imageAlignment())
                .commentsDisabled(commentsDisabled)
                .commentsHidden(commentsHidden)
                .build());
        return response(post, userId);
    }

    @Transactional
    public PostResponse update(Long postId, Long userId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Post post = requireOwner(postId, user);
        boolean commentsDisabled = user.getRole() == User.Role.ADMIN && request.commentsDisabled();
        boolean commentsHidden = user.getRole() == User.Role.ADMIN && request.commentsHidden();
        post.update(request.category(), request.title(), request.content(), join(request.hashtags()),
                join(request.imageUrls()), request.imageAlignment(), commentsDisabled, commentsHidden);
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
                ? postRepository.findAll(PageRequest.of(page, size))
                : postRepository.findByCategoryOrderByCreatedAtDesc(category, PageRequest.of(page, size));
        return posts.map(post -> response(post, userId));
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long postId, Long userId) {
        return response(postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)), userId);
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
        requirePost(postId);
        interactionRepository.toggleLike(postId, userId);
        return get(postId, userId);
    }

    @Transactional
    public PostResponse toggleBookmark(Long postId, Long userId) {
        Post post = requirePost(postId);
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
        requirePost(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(comment -> CommentResponse.from(comment, userId))
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CreateCommentRequest request) {
        Post post = requirePost(postId);
        if (post.isCommentsDisabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Comment comment = commentRepository.save(new Comment(post, user, request.content().trim()));
        return CommentResponse.from(comment, userId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentRepository.delete(comment);
    }

    private Post requirePost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private PostResponse response(Post post, Long userId) {
        return PostResponse.from(post, interactionRepository.likeCount(post.getId()),
                interactionRepository.commentCount(post.getId()), bookmarkRepository.countByPostId(post.getId()),
                interactionRepository.likedBy(post.getId(), userId),
                bookmarkRepository.existsByPostIdAndUserId(post.getId(), userId),
                post.getUser().getId().equals(userId));
    }

    private static String join(List<String> values) {
        if (values == null) return null;
        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(String::trim).reduce((a, b) -> a + "\n" + b).orElse(null);
    }
}
