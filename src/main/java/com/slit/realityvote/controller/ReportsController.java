package com.slit.realityvote.controller;

import com.slit.realityvote.dto.RankingRow;
import com.slit.realityvote.service.ReportsService;
import com.slit.realityvote.service.RealityShowService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.PrintWriter;
import java.util.List;

/**
 * Reporting Manager's module: dashboard stats, contestant rankings per
 * show, and CSV export. Deliberately read-only - reports summarize data
 * owned by other modules (Shows, Contestants, Votes) rather than
 * duplicating or re-storing it, so there's exactly one source of truth
 * for vote counts (the Vote table itself).
 */
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;
    private final RealityShowService showService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", reportsService.getDashboardStats());
        model.addAttribute("shows", showService.getAllActiveShows());
        return "reports/dashboard";
    }

    @GetMapping("/rankings")
    public String rankings(@RequestParam(required = false) Long showId, Model model) {
        model.addAttribute("shows", showService.getAllActiveShows());
        model.addAttribute("selectedShowId", showId);
        if (showId != null) {
            model.addAttribute("rankings", reportsService.getRankingsForShow(showId));
            model.addAttribute("showName", showService.getShowById(showId).getName());
        }
        return "reports/rankings";
    }

    /**
     * CSV export - written by hand rather than pulling in a library like
     * Apache POI, since CSV is plain text and the assignment lists CSV
     * as its own separate export format from Excel/PDF.
     */
    @GetMapping("/rankings/export")
    public void exportRankingsCsv(@RequestParam Long showId, HttpServletResponse response) throws Exception {
        List<RankingRow> rankings = reportsService.getRankingsForShow(showId);
        String showName = showService.getShowById(showId).getName();

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"rankings-" + showId + ".csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Rank,Contestant,Talent Category,Status,Total Votes,Avg Judge Score,Combined Score");
            for (RankingRow row : rankings) {
                writer.printf("%d,%s,%s,%s,%d,%s,%.1f%n",
                        row.rank(),
                        escapeCsv(row.contestantName()),
                        escapeCsv(row.talentCategory()),
                        row.status(),
                        row.totalVotes(),
                        row.avgJudgeScore() != null ? String.format("%.1f", row.avgJudgeScore()) : "",
                        row.combinedScore());
            }
        }
    }

    /** Wraps a field in quotes and escapes embedded quotes if it contains a comma or quote. */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
