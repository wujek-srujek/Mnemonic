#!/bin/bash -e

# generates .png images from .svg with the same names
# according to the sizes defined in the '<dir>/sizes' file
# requires Bash 4 and inkscape

ME="$(cd $(dirname $0); pwd -P)"
OUTDIR="${ME}/../src/main/res"
if [ ! -d "${OUTDIR}" ]; then
  echo "Resource directory '${OUTDIR}' does not exist."
  exit 1
fi

SIZES_DEF="sizes"

declare -A DENSITIES
DENSITIES[scaled]="$OUTDIR/drawable"
for density in m h xh xxh xxxh no; do
  DENSITIES["${density}"]="$OUTDIR/drawable-${density}dpi"
done

for dir in $(find . -type d); do
  if [ -f "$dir/$SIZES_DEF" ]; then
    (
      echo "Processing '$dir'"
      . "$dir/$SIZES_DEF"

      for density in "${!DENSITIES[@]}"; do
        var_w="${density}_w"
        var_h="${density}_h"

        if [ "${!var_w}" == "" -a "${!var_h}" == "" ]; then
          continue
        fi

        if [ "${!var_w}" == "" -o "${!var_h}" == "" ]; then
          echo "Error: '${density}_w' or '${density}_h' not defined for folder '$dir', skipping this density."
          continue
        fi

        density_dir="${DENSITIES[$density]}"
        echo "  $(basename "$density_dir")"
        if [ ! -d "$density_dir" ]; then
          echo "Creating missing '$density_dir' directory."
          mkdir "$density_dir"
        fi

        for file in $(find "$dir" -name '*.svg'); do
          basename="$(basename "$file")"
          echo "    '$basename'"
          outfilename="${basename%.svg}.png"
          inkscape -z -e "$density_dir/$outfilename" -w "${!var_w}" -h "${!var_h}" "$file" > /dev/null
        done
      done
    )
  fi
done
