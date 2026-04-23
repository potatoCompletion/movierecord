package com.my.movierecord.movie.controller;

import com.my.movierecord.movie.domain.Movie;
import com.my.movierecord.movie.dto.MovieForm;
import com.my.movierecord.movie.dto.MovieListItem;
import com.my.movierecord.movie.dto.SortOption;
import com.my.movierecord.movie.enums.Emotion;
import com.my.movierecord.movie.enums.Immersion;
import com.my.movierecord.movie.enums.Story;
import com.my.movierecord.movie.enums.Taste;
import com.my.movierecord.movie.service.MovieService;
import com.my.movierecord.common.service.FileStorageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@Controller
@RequestMapping("/movies")
public class MovieController {

    private static final int PAGE_SIZE = 20;

    private final MovieService movieService;
    private final FileStorageService fileStorageService;

    public MovieController(MovieService movieService, FileStorageService fileStorageService) {
        this.movieService = movieService;
        this.fileStorageService = fileStorageService;
    }

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

    @PostMapping
    public String create(@Valid @ModelAttribute("movieForm") MovieForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormReferences(model);
            model.addAttribute("mode", "create");
            model.addAttribute("existingThumbnailUrl", null);
            return "movies/form";
        }
        String thumbnailFilename = fileStorageService.store(thumbnail);
        movieService.create(form.toCommand(thumbnailFilename));
        redirectAttributes.addFlashAttribute("message", "영화가 등록되었습니다.");
        return "redirect:/movies";
    }

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
        boolean replaceThumbnail = thumbnail != null && !thumbnail.isEmpty();
        String thumbnailFilename = replaceThumbnail ? fileStorageService.store(thumbnail) : null;
        movieService.update(id, form.toCommand(thumbnailFilename), replaceThumbnail);
        redirectAttributes.addFlashAttribute("message", "영화가 수정되었습니다.");
        return "redirect:/movies";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        movieService.delete(id);
        redirectAttributes.addFlashAttribute("message", "영화가 삭제되었습니다.");
        return "redirect:/movies";
    }

    private void populateFormReferences(Model model) {
        model.addAttribute("immersionOptions", Immersion.values());
        model.addAttribute("storyOptions", Story.values());
        model.addAttribute("emotionOptions", Emotion.values());
        model.addAttribute("tasteOptions", Taste.values());
    }
}
