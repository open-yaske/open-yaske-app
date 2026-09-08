<!--
  Switch.svelte
  オン/オフのトグルスイッチ。
-->
<script lang="ts">
	interface Props {
		checked: boolean;
		onchange?: (checked: boolean) => void;
		label?: string;
		disabled?: boolean;
		id?: string;
	}

	let { checked = $bindable(false), onchange, label, disabled = false, id }: Props = $props();

	function toggle() {
		if (disabled) return;
		checked = !checked;
		onchange?.(checked);
	}
</script>

<label
	class="inline-flex items-center gap-3 cursor-pointer min-h-[44px] select-none"
	class:opacity-50={disabled}
	class:cursor-not-allowed={disabled}
>
	<button
		type="button"
		role="switch"
		aria-checked={checked}
		aria-label={label}
		{id}
		{disabled}
		onclick={toggle}
		class="relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full transition-colors duration-200 ease-[cubic-bezier(0.16,1,0.3,1)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2 active:scale-95"
		class:bg-[var(--color-primary-500)]={checked}
		class:bg-[var(--color-surface-card)]={!checked}
		class:backdrop-blur-md={!checked}
		class:shadow-[var(--shadow-style-sm)]={!checked}
	>
		<span
			class="inline-block h-5 w-5 transform rounded-full bg-white shadow-[var(--shadow-style-sm)] transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)] active:w-6"
			class:translate-x-5={checked}
			class:translate-x-0.5={!checked}
		></span>
	</button>
	{#if label}
		<span class="text-sm font-medium text-[var(--color-nav-active)]">{label}</span>
	{/if}
</label>
