export function formatRaceTime(time: string): string {
    const num = parseFloat(time);
    return isNaN(num) ? time : num.toFixed(2);
}
