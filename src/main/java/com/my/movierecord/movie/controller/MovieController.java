package com.my.movierecord.movie.controller;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.common.service.FileStorageService;
import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.dto.MovieForm;
import com.my.movierecord.movie.dto.MovieListItem;
import com.my.movierecord.movie.dto.SortOption;
import com.my.movierecord.movie.enums.Emotion;
import com.my.movierecord.movie.enums.Immersion;
import com.my.movierecord.movie.enums.Story;
import com.my.movierecord.movie.enums.Taste;
import com.my.movierecord.movie.service.MovieService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 영화 기록 관련 HTTP 요청을 처리하는 컨트롤러.
 * 영화 목록 조회, 생성, 수정, 삭제 기능을 제공한다.
 * 파일 업로드를 통한 썸네일 관리도 담당한다.
 */
@RequiredArgsConstructor
@Controller
@RequestMapping("/movies")
public class MovieController {

    private static final int PAGE_SIZE = 20;

    private final MovieService movieService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /**
     * 영화 목록을 페이지 단위로 조회하고 정렬하여 표시한다.
     * 정렬 옵션: latest(최신순), rating(별점순), title(제목순)
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "latest") String sort,
                       Model model) {
        SortOption sortOption = SortOption.from(sort);
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sortOption.getSort());
        Page<Movie> moviePage = movieService.list(pageable);

        List<MovieListItem> items = moviePage.getContent().stream()
                .map(MovieListItem::from)
                .toList();

        model.addAttribute("items", items);
        model.addAttribute("page", moviePage);
        model.addAttribute("currentSort", sortOption);
        model.addAttribute("sortOptions", SortOption.values());
        return "movies/list";
    }

    /**
     * 영화 생성 폼 페이지를 렌더링한다.
     * enum 옵션들(Immersion, Story, Emotion, Taste)을 폼에 추가한다.
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("movieForm")) {
            model.addAttribute("movieForm", new MovieForm());
        }
        populateFormReferences(model);
        model.addAttribute("mode", "create");
        model.addAttribute("existingThumbnailUrl", null);
        return "movies/form";
    }

    /**
     * 새 영화 기록을 생성한다.
     * 유효성 검증 실패 시 폼을 다시 표시한다.
     * 성공 시 영화를 저장하고 목록 페이지로 리다이렉트한다.
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("movieForm") MovieForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormReferences(model);
            model.addAttribute("mode", "create");
            model.addAttribute("existingThumbnailUrl", null);
            return "movies/form";
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));
        String thumbnailFilename = fileStorageService.store(thumbnail);
        movieService.create(form.toCommand(thumbnailFilename, user.getId()));
        redirectAttributes.addFlashAttribute("message", "영화가 등록되었습니다.");
        return "redirect:/movies";
    }

    /**
     * 영화 수정 폼 페이지를 렌더링한다.
     * 기존 영화 정보를 폼에 채우고 현재 썸네일 URL을 표시한다.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Movie movie = movieService.get(id);
        if (!model.containsAttribute("movieForm")) {
            model.addAttribute("movieForm", MovieForm.fromEntity(movie));
        }
        populateFormReferences(model);
        model.addAttribute("mode", "edit");
        model.addAttribute("movieId", id);
        model.addAttribute("existingThumbnailUrl",
                movie.getThumbnailPath() == null ? null : "/uploads/" + movie.getThumbnailPath());
        return "movies/form";
    }

    /**
     * 기존 영화 기록을 수정한다.
     * 썸네일이 새로 업로드되면 이전 썸네일을 삭제한다.
     * 유효성 검증 실패 시 폼을 다시 표시한다.
     */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("movieForm") MovieForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Movie movie = movieService.get(id);
            populateFormReferences(model);
            model.addAttribute("mode", "edit");
            model.addAttribute("movieId", id);
            model.addAttribute("existingThumbnailUrl",
                    movie.getThumbnailPath() == null ? null : "/uploads/" + movie.getThumbnailPath());
            return "movies/form";
        }
        // 썸네일이 새로 업로드되었는지 확인
        boolean replaceThumbnail = thumbnail != null && !thumbnail.isEmpty();
        String thumbnailFilename = replaceThumbnail ? fileStorageService.store(thumbnail) : null;
        movieService.update(id, form.toCommand(thumbnailFilename, null), replaceThumbnail);
        redirectAttributes.addFlashAttribute("message", "영화가 수정되었습니다.");
        return "redirect:/movies";
    }

    /**
     * 영화 기록을 삭제한다.
     * 관련된 썸네일 파일도 함께 삭제된다.
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        movieService.delete(id);
        redirectAttributes.addFlashAttribute("message", "영화가 삭제되었습니다.");
        return "redirect:/movies";
    }

    /**
     * 폼 렌더링에 필요한 enum 선택지들을 모델에 추가한다.
     */
    private void populateFormReferences(Model model) {
        model.addAttribute("immersionOptions", Immersion.values());
        model.addAttribute("storyOptions", Story.values());
        model.addAttribute("emotionOptions", Emotion.values());
        model.addAttribute("tasteOptions", Taste.values());
    }
}
