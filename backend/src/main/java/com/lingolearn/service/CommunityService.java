package com.lingolearn.service;

import com.lingolearn.common.BusinessException;
import com.lingolearn.common.PageResult;
import com.lingolearn.dto.*;
import com.lingolearn.entity.*;
import com.lingolearn.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 社区服务：帖子、评论、点赞 */
@Service
public class CommunityService {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;

    public CommunityService(PostRepository postRepository, PostCommentRepository commentRepository,
                            PostLikeRepository likeRepository, UserRepository userRepository,
                            LanguageRepository languageRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.languageRepository = languageRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<PostVO> posts(int page, int size, String languageCode, Long userId) {
        Page<Post> p;
        if (languageCode != null && !languageCode.trim().isEmpty()) {
            Language language = languageRepository.findByCode(languageCode.trim())
                    .orElseThrow(() -> new BusinessException(404, "语种不存在"));
            p = postRepository.findByLanguageIdOrderByCreatedAtDesc(language.getId(), PageRequest.of(page - 1, size));
        } else {
            p = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page - 1, size));
        }
        List<PostVO> vos = new ArrayList<>();
        for (Post post : p.getContent()) {
            vos.add(toPostVO(post, userId));
        }
        PageResult<PostVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(p.getTotalElements());
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    @Transactional
    public PostVO createPost(Long userId, CreatePostRequest req) {
        Post post = new Post();
        post.setUser(userRepository.getReferenceById(userId));
        post.setTitle(req.getTitle().trim());
        post.setContent(req.getContent().trim());
        if (req.getLanguageCode() != null && !req.getLanguageCode().trim().isEmpty()) {
            post.setLanguage(languageRepository.findByCode(req.getLanguageCode().trim())
                    .orElseThrow(() -> new BusinessException(404, "语种不存在")));
        }
        postRepository.save(post);
        return toPostVO(post, userId);
    }

    @Transactional(readOnly = true)
    public PostDetailVO postDetail(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        PostDetailVO vo = new PostDetailVO();
        vo.setPost(toPostVO(post, userId));
        List<CommentVO> comments = new ArrayList<>();
        for (PostComment c : commentRepository.findByPostIdOrderByCreatedAtAsc(postId)) {
            CommentVO cv = new CommentVO();
            cv.setId(c.getId());
            cv.setAuthorId(c.getUser().getId());
            cv.setAuthorNickname(c.getUser().getNickname());
            cv.setAuthorAvatar(c.getUser().getAvatar());
            cv.setContent(c.getContent());
            cv.setCreatedAt(c.getCreatedAt());
            comments.add(cv);
        }
        vo.setComments(comments);
        return vo;
    }

    @Transactional
    public CommentVO addComment(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(userRepository.getReferenceById(userId));
        comment.setContent(content.trim());
        commentRepository.save(comment);
        post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
        postRepository.save(post);
        User author = userRepository.getReferenceById(userId);
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setAuthorId(author.getId());
        vo.setAuthorNickname(author.getNickname() != null ? author.getNickname() : author.getUsername());
        vo.setAuthorAvatar(author.getAvatar());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        PostLike existing = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);
        boolean liked;
        if (existing != null) {
            likeRepository.delete(existing);
            post.setLikeCount(Math.max(0, (post.getLikeCount() == null ? 1 : post.getLikeCount()) - 1));
            liked = false;
        } else {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUser(userRepository.getReferenceById(userId));
            likeRepository.save(like);
            post.setLikeCount((post.getLikeCount() == null ? 0 : post.getLikeCount()) + 1);
            liked = true;
        }
        postRepository.save(post);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", post.getLikeCount());
        return result;
    }

    private PostVO toPostVO(Post post, Long userId) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        User author = post.getUser();
        vo.setAuthorId(author.getId());
        vo.setAuthorNickname(author.getNickname() != null ? author.getNickname() : author.getUsername());
        vo.setAuthorAvatar(author.getAvatar());
        if (post.getLanguage() != null) {
            vo.setLanguageId(post.getLanguage().getId());
            vo.setLanguageName(post.getLanguage().getNameCn());
            vo.setLanguageIcon(post.getLanguage().getIcon());
        }
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setCreatedAt(post.getCreatedAt());
        vo.setLiked(likeRepository.existsByPostIdAndUserId(post.getId(), userId));
        return vo;
    }
}