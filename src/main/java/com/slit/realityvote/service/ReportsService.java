package com.slit.realityvote.service;

import com.slit.realityvote.dto.DashboardStats;
import com.slit.realityvote.dto.RankingRow;

import java.util.List;

public interface ReportsService {

    DashboardStats getDashboardStats();

    /** Contestant rankings for one show, highest votes first, rank numbered from 1. */
    List<RankingRow> getRankingsForShow(Long showId);
}
