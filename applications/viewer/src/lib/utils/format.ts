/**
 * Format an amount in lovelace to ADA with proper decimal places
 * @param amount Amount in lovelace
 * @returns Formatted amount in ADA
 */
export function formatAmount(amount: number | string | null | undefined): string {
    if (amount === null || amount === undefined) return '0';
    
    const num = typeof amount === 'string' ? parseInt(amount) : amount;
    const ada = num / 1_000_000;
    
    // Format with commas and up to 6 decimal places
    return ada.toLocaleString('en-US', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 6
    });
} 