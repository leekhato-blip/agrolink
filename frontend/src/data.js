export const entityCatalog = {
  Supplier: [
    { id: 'S001', name: 'GreenFeed' },
    { id: 'S002', name: 'AquaFeeds' },
    { id: 'S003', name: 'Prime Agro Supplies' },
  ],
  Farm: [
    { id: 'F001', name: 'Farm Alpha' },
    { id: 'F002', name: 'Farm Beta' },
    { id: 'F003', name: 'Farm Delta' },
  ],
  Livestock: [
    { id: 'L001', name: 'Broiler batch' },
    { id: 'L002', name: 'Layers batch' },
    { id: 'L003', name: 'Broiler batch' },
    { id: 'L004', name: 'Broiler batch' },
    { id: 'L005', name: 'Layers batch' },
  ],
  FishPond: [
    { id: 'P001', name: 'Alpha Main Pond' },
    { id: 'P002', name: 'Delta Grow-Out Pond' },
  ],
  Feed: [
    { id: 'FE001', name: 'Layer Pro' },
    { id: 'FE002', name: 'Broiler Max' },
    { id: 'FE003', name: 'Fish Grower' },
    { id: 'FE004', name: 'Aqua Premium' },
  ],
  Disease: [
    { id: 'D001', name: 'Newcastle Disease' },
    { id: 'D002', name: 'Avian Influenza' },
    { id: 'D003', name: 'Fowl Pox' },
  ],
}

export const entityDescriptions = {
  Supplier: 'Trace feed supply chains and downstream farm impact.',
  Farm: 'See livestock, ponds, and shared supplier dependence.',
  Livestock: 'Inspect feed usage and disease exposure.',
  FishPond: 'Follow feed dependencies through aquaculture assets.',
  Feed: 'Reveal who supplies what and who depends on it.',
  Disease: 'Show disease exposure paths to feed suppliers.',
}

export const actionCatalog = [
  {
    id: 'direct',
    label: 'Direct connections',
    description: 'Show immediate linked entities.',
    types: ['Supplier', 'Farm', 'Livestock', 'FishPond', 'Feed', 'Disease'],
  },
  {
    id: 'supplier-dependency',
    label: 'Supplier dependency',
    description: 'Find farms that depend on a supplier through feed.',
    types: ['Supplier'],
  },
  {
    id: 'disease-to-supplier',
    label: 'Disease to supplier',
    description: 'Find suppliers connected to affected livestock.',
    types: ['Disease'],
  },
  {
    id: 'shared-suppliers',
    label: 'Shared suppliers',
    description: 'Find farms sharing a supplier via feed dependencies.',
    types: ['Farm'],
  },
  {
    id: 'ecosystem',
    label: 'Farm ecosystem',
    description: 'Explore connected entities within a hop limit.',
    types: ['Farm'],
  },
  {
    id: 'supplier-impact',
    label: 'Supplier impact analysis',
    description: 'Map the operational blast radius of a supplier outage.',
    types: ['Supplier'],
  },
]
