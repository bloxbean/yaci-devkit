<script lang="ts">
	let textareaprops = {
		id: 'message',
		name: 'message',
		label: 'Your message',
		rows: 30,
		cols: 100,
		background: '',
		placeholder: 'Leave a comment...'
	};

	export let text: any = {};
	export let rows: number = 30;
	export let cols: number = 100;
	export let background: string = '';

	function toPrettyJson(obj: any): string {
		if (!obj) return '{}';
		return JSON.stringify(obj, null, 2);
	}

	textareaprops = { ...textareaprops, rows, cols, background };
</script>

<div class="p-4 w-full w">
	<div class="flex flex-col mt-4">
		<div class="md:flex-grow">
			<textarea
				{...textareaprops}
				value={toPrettyJson(text)}
				class={`w-full h-max textarea-info text-sm md:text-base ${background}`}
				disabled
			></textarea>
		</div>
	</div>
</div>
