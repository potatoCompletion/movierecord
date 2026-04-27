package com.my.movierecord.movie.controller;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.dto.NicknameUpdateForm;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.auth.service.UserService;
import com.my.movierecord.movie.dto.MovieListItem;
import com.my.movierecord.movie.dto.SortOption;
import com.my.movierecord.movie.service.MovieService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequiredArgsConstructor
@Controller
@RequestMapping("/my-page")
public class MyPageController {

    private static final int PAGE_SIZE = 20;
    private static final java.util.regex.Pattern NICKNAME_PATTERN =
            java.util.regex.Pattern.compile("^[가-힣a-zA-Z0-9_]+$");

    private final MovieService movieService;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    public String myPage(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "latest") String sort,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        SortOption sortOption = SortOption.from(sort);
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sortOption.getSort());

        var moviePage = movieService.listByUser(user.getId(), pageable);
        List<MovieListItem> items = moviePage.getContent().stream()
                .map(MovieListItem::from)
                .toList();

        model.addAttribute("items", items);
        model.addAttribute("page", moviePage);
        model.addAttribute("currentSort", sortOption);
        model.addAttribute("sortOptions", SortOption.values());
        model.addAttribute("activeTab", "movies");
        return "movies/my-page";
    }

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        model.addAttribute("nicknameForm", new NicknameUpdateForm());
        model.addAttribute("currentNickname", user.getDisplayNickname());
        model.addAttribute("activeTab", "profile");
        return "movies/my-page";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("nicknameForm") NicknameUpdateForm form,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        if (!bindingResult.hasErrors() && !userService.isNicknameAvailable(form.getNickname(), user.getId())) {
            bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentNickname", user.getDisplayNickname());
            model.addAttribute("activeTab", "profile");
            return "movies/my-page";
        }

        try {
            userService.updateNickname(user.getId(), form.getNickname());
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request claimed the nickname between the check and the write
            bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
            model.addAttribute("currentNickname", user.getDisplayNickname());
            model.addAttribute("activeTab", "profile");
            return "movies/my-page";
        }

        return "redirect:/my-page/profile?success";
    }

    @GetMapping("/check-nickname")
    @ResponseBody
    public Map<String, Object> checkNickname(@RequestParam String nickname,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> result = new HashMap<>();
        if (nickname == null || nickname.isBlank()
                || nickname.length() < 2 || nickname.length() > 50
                || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            result.put("available", false);
            result.put("message", "유효하지 않은 닉네임 형식입니다.");
            return result;
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));
        boolean available = userService.isNicknameAvailable(nickname, user.getId());
        result.put("available", available);
        result.put("message", available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.");
        return result;
    }
}
