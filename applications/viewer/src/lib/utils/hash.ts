/**
 * Truncate a hash or address to a shorter format
 * @param address The full hash or address string
 * @param startLength Number of characters to show at start (default: 8)
 * @param endLength Number of characters to show at end (default: 8)
 * @returns Truncated string with ellipsis
 */
export function truncateAddress(address: string | null | undefined, startLength = 8, endLength = 8): string {
    if (!address) return '';
    if (address.length <= startLength + endLength) return address;
    
    return `${address.slice(0, startLength)}...${address.slice(-endLength)}`;
} 