import type { PageLoad } from './$types'

export const load: PageLoad = async ({params}) => {

    let splits = params.actionid.split('_');
    let index = parseInt(splits[1]);
    const INDEXER_BASE_URL = import.meta.env.VITE_INDEXER_BASE_URL;
    const blockApiUrl = `${INDEXER_BASE_URL}/gov-action-proposals/${splits[0]}`;

    const res = await fetch(blockApiUrl);
    const govActions = await res.json();

    if (res.ok) {
        let filterActionArr = govActions.filter((govAction) => govAction.index === index);
        if (filterActionArr.length === 0) {
            return {
                status: 404,
                body: { error: 'Can not fetch Gov Action Proposal.' }
            };
        } else {
            return {
                govAction: filterActionArr[0]
            };
        }
    }

    return {
        status: 404,
        body: { error: 'Can not fetch block.' }
    };
}
