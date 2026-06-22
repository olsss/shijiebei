export function formatMarketLine(lineValue?: string | null, handicapLine?: number | null): string {
  const textLine = lineValue?.trim();
  if (textLine) {
    return textLine;
  }
  return handicapLine == null ? '-' : String(handicapLine);
}
