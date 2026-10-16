/* The address under the pointer, so every input and output at that address lights up together. */
class AddressHover {
	address = $state<string | null>(null);
}

export const hover = new AddressHover();
