#
# = [Beta] Aspell Speedy Check module =
#
#	[
#	|*| Source: https://github.com/MasterInQuestion/ShellUtils/blob/main/sc
#	|*| Last update: CE 2021-05-15 11:26 UTC ]
#
# ----
#
#	An Unix Shell script wrapper delegating to Aspell performing speedy spell check against the list of input files to ensure that all had been spelled out grammarly.
#	Don't know anything particular to check? `sc /` works.
#	(assuming the script was named "sc" and put effective under a directory of "$PATH")
#
#	Compatibility Note:
#	This script should work with all modern reasonable Unix shells.
#	(tested with BusyBox environment; using its `ash`)
#
#	Beta status:
#	Old work mostly complete with some undone improvements.
#	|1| Documentation.
#	|2| Proper handling for STDIN input. (current implementation asserted file input)
#	|3| Pipe input, and Null-separated single input list: shall be better handled.
#	(typically that from `find -print0` output)
#
#
#
#
# == Implementation ==
#

# === Environment ===
#
# Modify the utility paths here to their corresponding value if necessary:
# [[
	aspell='/usr/bin/aspell';
	GNU_CoreUtils_readlink='/usr/bin/readlink';
	GNU_grep='/usr/bin/grep';
	perl='/usr/bin/perl';
# ]]
#

	printHelp () {
	rm -rf "$previouslyAddedDirs" "$files" "$filesToAdd"&
	cat "$0";
	exit;
	};

	inputEscape () {
# [Note 1]
	PERLIO=':raw' "$perl" -p \
	-e 'BEGIN { $^H |= 0x02000008; $^H{reflags_charset} = 4; $/ = "\x00"; }' \
	-e '
	s/(?='\'')/'\''\\'\''/g;
	s/^\x00*/'\''/;
	s/\x00*$/'\'' /;
	';
	};

	checkDuplicate () {
	x0="'${x0//\'/"'\\''"}'";
	"$GNU_grep" -zF -e "$x0" -q "$1"; # [Note 2]
	};

	checkDuplicateStdin () {
	x0="'${x0//\'/"'\\''"}'";
	{ "$GNU_grep" -zF -e "$x0" -q <<EOF
$1
EOF
	}; # [Note 2] [Note 3]
	};


	tempDir="$( readlink -f "${TMP:-"${TMPDIR:-"/tmp"}"}" )" || exit;

	previouslyAddedDirs="$tempDir"'/[Aspell Speedy Check] Temp file: "$previouslyAddedDirs"';
	files="$tempDir"'/[Aspell Speedy Check] Temp file: "$files"';
	filesToAdd="$tempDir"'/[Aspell Speedy Check] Temp file: "$filesToAdd"';

	unset tempDir;

	{
	echo -n >| "$previouslyAddedDirs" &&
	echo -n >| "$files" && {
	rm -rf "$filesToAdd";
	mkfifo -m 600 "$filesToAdd";
	};

	} || {
	rm -rf "$previouslyAddedDirs" "$files"&
	exit;
	};


# === Argument parsing ===

	[ $# -ne 0 ] || printHelp;


	for x0;

	do {
	[ -n "$x0" ] || continue;

# >>>>
	if
	[ -z $__ ] && {
	[ "$x0" != '--' ] || { __=1; continue; };
	} &&
# Whatever starting with "-" (before 1st "--") would be considered an "option":
	{ grep -E -e '^-' -q <<EOF
$x0
EOF
	};
# [ ^
#	Using Shell string manipulation and comparison for this case:
#	E.g. {
#	x1="${x0#-}";
#	[ ${#x0} -ne ${#x1} ];
#	};
#	; may achieve better performance. (whatsoever marginally)
#
#	The `grep` here is mostly demonstrational. ]


	then {

# ==== Options ====

	[ "$x0" != '--help' ] || printHelp;
	options="$options '${x0//\'/"'\\''"}'";
	};


	else {

# ==== Files ====

# >>>> (1)
	x0="$( readlink -f -- "$x0" )";

	if
	[ "$x0" != '/' ];

# ===== Normal case =====

	then {
	if
	[ -d "$x0" ];

	then
# >>>> (2)
	checkDuplicate "$previouslyAddedDirs" || {
	echo -nE "$x0 " >> "$previouslyAddedDirs"; # [Note 4]
	x1="$( cat "$files" )";

	eval '
	for x0 in \
	'"$(
	eval '
	find '"$x0"' -follow -type f -print0 |
# GNU CoreUtils `readlink` would be required to have it work normally:
	xargs -0r '\'"${GNU_CoreUtils_readlink//\'/"'\\''"}"\'' -fz |
	sort -uz | # [Note 5]
	inputEscape;
	';
	)"';

	do
	checkDuplicateStdin "$x1" || echo -nE "$x0 "; # [Note 4]

	done > "$filesToAdd"&
	';

	x0="$( cat "$filesToAdd" )";
	echo -nE "$x0" >> "$files"; # [Note 4]
	};
# <<<< (2)

	else
	checkDuplicate "$files" || echo -nE "$x0 " >> "$files"; # [Note 4]

	fi;
	};

# ===== Root directory =====

	else
# >>>> (2)
	grep -F -e "'/'" -q "$previouslyAddedDirs" || {
	echo -nE "'/' " >> "$previouslyAddedDirs"; # [Note 4]

	x1="$(
	find '/' -type f -print0 |
	sort -z | # [Note 5]
	inputEscape;
	)";

	eval '
	for x0 in \
	'"$( cat "$files" )"';

	do
	checkDuplicateStdin "$x1" || echo -nE "$x0 "; # [Note 4]

	done > "$filesToAdd"&
	';

	x0="$( cat "$filesToAdd" )";
	echo -nE "${x0}${x1} " >| "$files"; # [Note 4]

	unset x1;
	};
# <<<< (2)

	fi;
# <<<< (1)
	};

	fi;
# <<<<
	};

	done;


	x1="$( cat "$files" )";
	rm -rf "$previouslyAddedDirs" "$files" "$filesToAdd"&
#
#	Commented to workaround some problematic shell string replace behavior on lengthy input: [Note 6]
# [[
#	previouslyAddedDirs="'${previouslyAddedDirs//\'/"'\\''"}'";
#	files="'${files//\'/"'\\''"}'";
#
#	x1="${x1/"$previouslyAddedDirs "}";
#	x1="${x1/"$files "}";
# ]]
# (no harm except possibly outputting 2 more meaningless entries on fringe cases)
#
	unset previouslyAddedDirs files filesToAdd;


# === Delegating to Aspell ===

	eval '
	unset options;

	for file in \
	'"$x1"';


	do
	x0="$(
# Check `aspell --help` (or just googling) for parameter explanation.
	'\'"${aspell//\'/"'\\''"}"\'' \
	--encoding=UTF-8 \
	--mode=url \
	--lang=en_US \
	--norm-form=none \
	--ignore=1 \
	--ignore-case=false \
	--dont-run-together \
	--run-together-limit=2 \
	--run-together-min=3 \
	--camel-case=false \
	'"$options"' \
	list \
	2>&1 < "$file";
	)";
	x1=$?;

# Print only when `aspell` outputs non-empty:
	[ -n "$x0" ] && {
	fileEscaped="${file%\\}";

	if
	[ ${#file} -eq ${#fileEscaped} ];

	then
	fileEscaped="${fileEscaped//\"/\\\"}";

	else
	fileEscaped="${fileEscaped//\"/\\\"}\\\\";

	fi;

	printf '\''|*| "%s":\n'\'' "$fileEscaped";
	echo -E '\''[['\''; # [Note 4]

	if
	[ $x1 -eq 0 ];

	then
# Sorting and deduplication:
	printf '\''%s\n'\'' "$( sort -u <<EOF
$x0
EOF
	)";

	else
	printf '\''%s\n'\'' "$x0";

	fi;

	echo -e '\'']]\n'\''; # [Note 4]
	};


	done;

	unset file fileEscaped x0 x1;
	';
#
#
#
#
# == Notes & References ==
#
#
# === Notes ===
#
# [ [Note 1]
#	Perl magic related.
#	PLACEHOLDER
#	https://github.com/MasterInQuestion/perlp ]
#
# [ [Note 2]
#	The checking here would not work as intended, if the input was of some silly filenames that shall contain "\n" (Line Feed) (U+000A, 0x0A, &#10;): [ Also check: [Note 5] ]
#	As `grep` normally operates on a "per-line" basis (thus the embedding "\n" cannot be matched normally).
#
#	To fix the problem would require using the GNU `grep`:
# [[
#	grep -zF -e ...
# ]]
# (applying the "-z" flag: choosing "\x00" (Null) (U+0000, 0x00, &#0;) as the record separator instead of "\n") ]
#
# [ [Note 3]
#	Removing the part:
# [[
#	{ grep -F -e "$x0" -q <<EOF
# $1
# EOF
#	};
# ]]
# (skipping the duplicate check on previously added files)
#	Can significantly speed up directory handling. (mostly for ones that contain many items)
#
#	Off-Topic:
#	Despite the logic has been highly optimized, due to its actuation being expensive:
#	The commands may be actually slower, than simply adding everything blindly then perform a `sort -uz` at the last stage (before delegating to Aspell). ]
#
# [ [Note 4]
#	Another problem is that some unreasonable shells may not recognize the "-e" / "-E" flags of `echo` normally:
#	Regarding them as normal output content and performing escape sequence interpretation unconditionally: resulting in unwanted output.
#
#	`printf` should work portably, but which might cause unnecessary performance overhead:
#	|1| It's not necessarily a shell built-in (while `echo` is, in pretty much all cases).
#	; which means whose execution can be much more expensive.
#	(a prominent example here: `mksh`)
#	|2| Interpreting the `printf` formatting directive is regardless more complex:
#	; i.e. more work has to be done; than to simply `echo` something directly.
#	(despite the usually marginal performance difference) ]
#
# [ [Note 5]
#	As of BusyBox v1.33.0, `sort -z` doesn't work properly.
#	(incorrect record splitting behavior with "\n")
#	More details: ${URL} ]
#
# [ [Note 6]
#	Actually significant speed-up can be noticed in general cases. (with BusyBox `ash`) ]
#
#
# === References ===
#
# [ [1]
#	PLACEHOLDER ]
#
