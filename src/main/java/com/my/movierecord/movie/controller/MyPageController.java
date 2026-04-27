package com.my.movierecord.movie.controller;

import com.my.movierecord.auth.domain.User;
import com.my.movierecord.auth.repository.UserRepository;
import com.my.movierecord.movie.dto.MovieListItem;
import com.my.movierecord.movie.dto.SortOption;
import com.my.movierecord.movie.service.MovieService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
@RequestMapping("/my-page")
public class MyPageController {

    private static final int PAGE_SIZE = 20;

    private final MovieService movieService;
    private final UserRepository userRepository;

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
        return "movies/my-page";
    }
}
