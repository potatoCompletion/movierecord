package com.my.movierecord.record.controller;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.record.domain.WatchRecord;
import com.my.movierecord.record.dto.*;
import com.my.movierecord.record.enums.Emotion;
import com.my.movierecord.record.enums.Immersion;
import com.my.movierecord.record.enums.Story;
import com.my.movierecord.record.enums.Taste;
import com.my.movierecord.record.service.WatchRecordService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/records")
public class RecordController {

    private static final int PAGE_SIZE = 20;

    private final WatchRecordService watchRecordService;
    private final UserRepository userRepository;

    public RecordController(WatchRecordService watchRecordService, UserRepository userRepository) {
        this.watchRecordService = watchRecordService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "latest") String sort,
                       @RequestParam(defaultValue = "all") String filter,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {
        if ("mine".equals(filter) && userDetails == null) {
            return "redirect:/auth/login";
        }

        SortOption sortOption = SortOption.from(sort);
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sortOption.getSort());

        Long currentUserId = null;
        boolean isAdmin = false;
        RecordPageDto recordPageDto;

        if (userDetails != null) {
            User user = currentUser(userDetails);
            currentUserId = user.getId();
            isAdmin = "ROLE_ADMIN".equals(user.getRole());
            recordPageDto = "mine".equals(filter)
                    ? watchRecordService.listByUser(user.getId(), pageable)
                    : watchRecordService.list(pageable);
        } else {
            recordPageDto = watchRecordService.list(pageable);
        }

        Page<WatchRecord> recordPage = recordPageDto.getPage();
        List<RecordListItem> items = recordPageDto.getItems();

        model.addAttribute("items", items);
        model.addAttribute("page", recordPage);
        model.addAttribute("currentSort", sortOption);
        model.addAttribute("sortOptions", SortOption.values());
        model.addAttribute("currentFilter", filter);
        model.addAttribute("recordCount", recordPage.getTotalElements());
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("isAdmin", isAdmin);
        return "records/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        WatchRecord record = watchRecordService.get(id);
        Long currentUserId = null;
        boolean isAdmin = false;
        if (userDetails != null) {
            User user = currentUser(userDetails);
            currentUserId = user.getId();
            isAdmin = "ROLE_ADMIN".equals(user.getRole());
        }
        model.addAttribute("item", RecordDetail.from(record));
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("isAdmin", isAdmin);
        return "records/detail";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long tmdbId,
                          @RequestParam(required = false) String mediaType,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) String posterPath,
                          Model model) {
        if (!model.containsAttribute("movieForm")) {
            RecordForm form = new RecordForm();
            form.setTmdbId(tmdbId);
            form.setMediaType(mediaType);
            form.setTitle(title);
            form.setPosterPath(posterPath);
            model.addAttribute("movieForm", form);
        }
        populateFormReferences(model);
        model.addAttribute("mode", "create");
        String thumbnailUrl = (posterPath != null && !posterPath.isBlank())
                ? "https://image.tmdb.org/t/p/w342" + posterPath : null;
        model.addAttribute("existingThumbnailUrl", thumbnailUrl);
        return "records/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("movieForm") RecordForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormReferences(model);
            model.addAttribute("mode", "create");
            model.addAttribute("existingThumbnailUrl", null);
            return "records/form";
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));
        watchRecordService.create(form.toCommand(user.getId()));
        redirectAttributes.addFlashAttribute("success", "감상평이 등록되었습니다.");
        return "redirect:/records";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        WatchRecord record = watchRecordService.get(id);
        User user = currentUser(userDetails);
        if (!isAuthorized(record, user)) {
            redirectAttributes.addFlashAttribute("error", "본인의 감상평만 수정할 수 있습니다.");
            return "redirect:/records";
        }
        if (!model.containsAttribute("movieForm")) {
            model.addAttribute("movieForm", RecordForm.fromEntity(record));
        }
        populateFormReferences(model);
        model.addAttribute("mode", "edit");
        model.addAttribute("movieId", id);
        model.addAttribute("existingThumbnailUrl", thumbnailUrl(record));
        return "records/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("movieForm") RecordForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        WatchRecord record = watchRecordService.get(id);
        User user = currentUser(userDetails);
        if (!isAuthorized(record, user)) {
            redirectAttributes.addFlashAttribute("error", "본인의 감상평만 수정할 수 있습니다.");
            return "redirect:/records";
        }
        if (bindingResult.hasErrors()) {
            populateFormReferences(model);
            model.addAttribute("mode", "edit");
            model.addAttribute("movieId", id);
            model.addAttribute("existingThumbnailUrl", thumbnailUrl(record));
            return "records/form";
        }
        watchRecordService.update(id, form.toCommand(null));
        redirectAttributes.addFlashAttribute("success", "감상평이 수정되었습니다.");
        return "redirect:/records";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        WatchRecord record = watchRecordService.get(id);
        User user = currentUser(userDetails);
        if (!isAuthorized(record, user)) {
            redirectAttributes.addFlashAttribute("error", "본인의 감상평만 삭제할 수 있습니다.");
            return "redirect:/records";
        }
        watchRecordService.delete(id);
        redirectAttributes.addFlashAttribute("success", "감상평이 삭제되었습니다.");
        return "redirect:/records";
    }

    private User currentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));
    }

    private boolean isAuthorized(WatchRecord record, User user) {
        return "ROLE_ADMIN".equals(user.getRole())
                || record.getUser().getId().equals(user.getId());
    }

    private void populateFormReferences(Model model) {
        model.addAttribute("immersionOptions", Immersion.values());
        model.addAttribute("storyOptions", Story.values());
        model.addAttribute("emotionOptions", Emotion.values());
        model.addAttribute("tasteOptions", Taste.values());
    }

    private String thumbnailUrl(WatchRecord record) {
        if (record.getContent() == null || record.getContent().getThumbnailPath() == null) {
            return null;
        }
        return "/uploads/" + record.getContent().getThumbnailPath();
    }
}
