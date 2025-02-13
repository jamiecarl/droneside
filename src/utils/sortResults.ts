import type { ResultSummaryType } from 'types/events.vue';

export function sortResultsByPosition(results: ResultSummaryType[]) {
    return results.sort((a, b) => {
        if (a.Position === null) return 1;
        if (b.Position === null) return -1;
        return Number(a.Position) - Number(b.Position);
    });
}

export function sortResultByPbTime(results: ResultSummaryType[]) {
    const sorted = results.sort((a, b) => parseFloat(a.PbLapTime || "999") - parseFloat(b.PbLapTime || "999"));
    sorted.forEach((result, index) => {
        result.Position = (index + 1).toString();
    });
    return sorted.slice(0, 3);
}