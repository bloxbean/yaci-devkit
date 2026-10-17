import type { Component } from 'svelte';
import LayoutDashboard from '@lucide/svelte/icons/layout-dashboard';
import Blocks from '@lucide/svelte/icons/blocks';
import ArrowLeftRight from '@lucide/svelte/icons/arrow-left-right';
import Wallet from '@lucide/svelte/icons/wallet';
import Coins from '@lucide/svelte/icons/coins';
import Server from '@lucide/svelte/icons/server';
import Gift from '@lucide/svelte/icons/gift';
import Layers from '@lucide/svelte/icons/layers';
import ArrowDownLeft from '@lucide/svelte/icons/arrow-down-left';
import PiggyBank from '@lucide/svelte/icons/piggy-bank';
import BadgeCheck from '@lucide/svelte/icons/badge-check';
import Landmark from '@lucide/svelte/icons/landmark';
import ScrollText from '@lucide/svelte/icons/scroll-text';
import Users from '@lucide/svelte/icons/users';
import Vote from '@lucide/svelte/icons/vote';
import Scale from '@lucide/svelte/icons/scale';
import Gavel from '@lucide/svelte/icons/gavel';
import Link2 from '@lucide/svelte/icons/link-2';
import Timer from '@lucide/svelte/icons/timer';
import UserPlus from '@lucide/svelte/icons/user-plus';

export interface NavItem {
	label: string;
	href: string;
	icon: Component;
	/** Other paths that should highlight this item. */
	match?: string[];
	keywords?: string;
}

export interface NavGroup {
	label: string;
	items: NavItem[];
}

export const NAV: NavGroup[] = [
	{
		label: 'Explore',
		items: [
			{ label: 'Dashboard', href: '/', icon: LayoutDashboard, keywords: 'home live overview' },
			{ label: 'Blocks', href: '/blocks', icon: Blocks },
			{ label: 'Transactions', href: '/transactions', icon: ArrowLeftRight, keywords: 'txs' },
			{ label: 'Epochs', href: '/epochs', icon: Timer, keywords: 'protocol parameters params' },
			{ label: 'Addresses', href: '/addresses', icon: Wallet, match: ['/stake'], keywords: 'wallet balance utxo' },
			{ label: 'Assets', href: '/assets', icon: Coins, keywords: 'tokens nft policy mint' }
		]
	},
	{
		label: 'Staking',
		items: [
			{ label: 'Pools', href: '/pools/search', icon: Server, match: ['/pools'], keywords: 'stake pool spo' },
			{ label: 'Delegations', href: '/certificates/delegations', icon: Link2 },
			{ label: 'Rewards', href: '/rewards', icon: Gift },
			{ label: 'Epoch stakes', href: '/epoch-stakes', icon: Layers },
			{ label: 'Withdrawals', href: '/withdrawals', icon: ArrowDownLeft },
			{ label: 'Ada pots', href: '/adapot/current', icon: PiggyBank, match: ['/adapot'], keywords: 'treasury reserves supply' },
			{
				label: 'Certificates',
				href: '/certificates/stakekey-registrations',
				icon: BadgeCheck,
				match: ['/certificates/stakekey', '/certificates/pool'],
				keywords: 'registrations deregistrations retirements'
			}
		]
	},
	{
		label: 'Governance',
		items: [
			{ label: 'Overview', href: '/governance', icon: Landmark, keywords: 'conway' },
			{ label: 'Proposals', href: '/governance/proposals', icon: ScrollText, match: ['/governance/govactions'], keywords: 'gov actions' },
			{ label: 'DReps', href: '/governance/dreps', icon: Users, keywords: 'delegated representatives' },
			{ label: 'DRep activity', href: '/governance/drep-registrations', icon: UserPlus, match: ['/governance/drep-'], keywords: 'drep registrations updates retirements' },
			{ label: 'Votes', href: '/governance/votes', icon: Vote },
			{ label: 'Committee', href: '/governance/committee', icon: Gavel, keywords: 'cc constitutional' },
			{ label: 'Constitution', href: '/governance/constitution', icon: Scale }
		]
	}
];

export const ALL_PAGES: NavItem[] = NAV.flatMap((g) => g.items);

/** The nav item for a path: the one whose href or match prefix is the longest match. */
export function activeHref(path: string): string | null {
	let best: { href: string; len: number } | null = null;
	for (const item of ALL_PAGES) {
		for (const root of [item.href, ...(item.match ?? [])]) {
			const hit = root === '/' ? path === '/' : path.startsWith(root);
			if (hit && (!best || root.length > best.len)) best = { href: item.href, len: root.length };
		}
	}
	return best?.href ?? null;
}
