<img src="src/main/resources/pluginIcon.svg" width="80" height="80" alt="icon" align="left"/>

VimMulticursor
===
![Build](https://github.com/dankinsoid/VimMulticursor/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/19162-VimMulticursor.svg)](https://plusins.jetbrains.com/plugin/19162-VimMulticursor)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/19162-VimMulticursor.svg)](https://plugins.jetbrains.com/plugin/19162-VimMulticursor)

<!-- Plugin description -->
This plugin adds multiple cursor and selection capabilities to `IdeaVim`

![Preview](https://github.com/dankinsoid/VimMulticursor/blob/main/preview.gif?raw=true)

## Installation & Setup

1. Install the plugin:
   - Open IntelliJ IDEA
   - Go to Settings/Preferences → Plugins → Marketplace
   - Search for "VimMulticursor"
   - Click Install and restart IDE

2. Enable the plugin:
   - Add the following line to the top of your `~/.ideavimrc` file:
     ```
     set multicursor
     ```
   - Reload IdeaVim settings (`:action IdeaVim.ReloadVimRc`)

## Commands

### Basic Usage
- `mc` + command: Create multiple cursors
- `ms` + command: Create multiple selections
- All commands work within selected text when there's an active selection

### Available Commands
- `/regex-pattern`: Search for regex pattern
- `f{char}`: Find character forward
- Text Objects:
  - `aw`: Around word (includes trailing space)
  - `iw`: Inside word
  - `ab`: Around brackets ()
  - `ib`: Inside brackets ()
  - `aB`: Around braces {}
  - `iB`: Inside braces {}
- `F{char}`: Find character backward
- `t{char}`: Till before character forward
- `T{char}`: Till before character backward
- `w`: Next word start
- `W`: Next WORD start
- `b`: Previous word start
- `B`: Previous WORD start
- `e`: Next word end
- `E`: Next WORD end

### Cursor Management
- `mcc`: Add/remove a cursor highlight at current position (preview mode)
- `mcr`: Convert cursor highlights into active editing cursors
- `mcd`: Remove all cursors and highlights
- `mcia`: Inside any brackets/quotes ((), [], {}, "", '', ``)
- `mcaa`: Around any brackets/quotes
- `mcaw`: Add cursors at word boundaries (start and end of current word)

The `mcia` and `mcaa` commands automatically find the nearest matching pair of delimiters around the cursor, handling proper nesting and matching of brackets/quotes.

## Examples

1. Select all occurrences of "print":
   ```
   ms/print<Enter>
   ```

2. Create cursors at each word start in selection:
   ```
   msw
   ```

3. Add cursors at specific positions:
   1. Move cursor to desired position
   2. Type `mcc` to add a cursor highlight
   3. Repeat for more positions
   4. Type `mcr` to convert highlights into editing cursors

## Optional Key Mappings

Add these to your `~/.ideavimrc` for faster access:
```vim
" Quick search-select
map q <Plug>(multicursor-ms/)

" Quick cursor add/apply
map z <Plug>(multicursor-mcc)
map Z <Plug>(multicursor-mci)

" Word-based selections
map <leader>w <Plug>(multicursor-msw)
map <leader>b <Plug>(multicursor-msb)
```

## Troubleshooting

1. Commands not working?
   - Ensure `set multicursor` is at the top of `.ideavimrc`
   - Restart IdeaVim after changes
   - Check if IdeaVim plugin is enabled

2. Cursors not appearing?
   - Make sure you're in normal mode
   - Try clearing cursors with `mcd`
   - Restart IDE if issues persist
<!-- Plugin description end -->

## License

Just as IdeaVim, this plugin is licensed under the terms of the GNU Public License version 3 or any later version.

## Credits

Plugin icon is merged icons of IdeaVim plugin and a random sneaker by FreePic from flaticon.com
## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "VimMulticursor"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/dankinsoid/VimMulticursor/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
